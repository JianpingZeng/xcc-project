package tools.commandline;
/*
 * Extremely C language Compiler.
 * Copyright (c) 2015-2019, Jianping Zeng.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied.  See the License for the specific language governing
 * permissions and limitations under the License.
 */

import backend.target.Target;
import backend.target.Target.TargetRegistry;
import config.Config;
import tools.Error;
import tools.OutRef;
import tools.Pair;
import tools.Util;

import java.io.File;
import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;

import static java.lang.System.out;
import static java.util.stream.Collectors.toList;
import static tools.commandline.Desc.desc;
import static tools.commandline.FormattingFlags.*;
import static tools.commandline.MiscFlags.*;
import static tools.commandline.NumOccurrences.*;
import static tools.commandline.OptionHidden.Hidden;
import static tools.commandline.OptionHidden.ReallyHidden;
import static tools.commandline.OptionNameApplicator.optionName;
import static tools.commandline.ValueExpected.ValueDisallowed;
import static tools.commandline.ValueExpected.ValueRequired;

/**
 * @author Jianping Zeng
 * @version 0.4
 */
public final class CL {
  /**
   * The overview about the main program.
   */
  public static String ProgramOverview = null;
  /**
   * The name of main program.
   */
  public static String ProgramName = "<premain>";

  /**
   * This is the head of single linked list which chained all of registered option.
   */
  public static Option<?> registeredOptionList = null;
  /**
   * Indicates whether the registered option list is changed due to an option is
   * added.
   */
  public static boolean optionListChanged = false;

  public static void markOptionsChanged() {
    optionListChanged = true;
  }

  /**
   * A entry to {@linkplain #parseCommandLineOptions(String[], String)} with
   * empty overview string.
   *
   * @param args
   * @throws Exception
   */
  public static void parseCommandLineOptions(String[] args) {
    parseCommandLineOptions(args, "");
  }

  /**
   * Before Calling this method, must make sure that the first argument
   * is the name of executable file calling this Java Main Program (It usually
   * be a C/C++ native program name).
   *
   * @param args
   * @throws Exception
   */
  public static void parseCommandLineOptions(String[] args, String overview) {
    ArrayList<Option> positionalOpts = new ArrayList<>();
    ArrayList<Option> sinkOpts = new ArrayList<>();
    HashMap<String, Option> optionsMap = new HashMap<>();

    // Process all registered options.
    getOptionInfo(positionalOpts, sinkOpts, optionsMap);

    Util.assertion(!(optionsMap.isEmpty() && positionalOpts.isEmpty()), "No options specified");


    // Copy the program name into ProgName, making sure not to overflow it.
    if (args[0].equals("launcher")) {
      // Note that:
      // Set the first argument as 'launcher' to inform the CL.parseCommandLineOptions
      // we calling it by native launcher.
      ProgramName = new File(args[1]).getName();
      ProgramOverview = overview;

      // Avoiding the first argument that represents program name.
      String[] temp = new String[args.length - 2];
      System.arraycopy(args, 2, temp, 0, temp.length);
      args = temp;
    }
    boolean ErrorParsing = false;

    // Check out the positional arguments to collect information about them.
    int NumPositionalRequired = 0;

    // Determine whether or not there are an unlimited number of positionals
    boolean HasUnlimitedPositionals = false;

    Option ConsumeAfterOpt = null;
    if (!positionalOpts.isEmpty()) {
      if (positionalOpts.get(0).getNumOccurrencesFlag() == ConsumeAfter) {
        Util.assertion(positionalOpts.size() > 1, "Cannot specify ConsumeAfter without a positional argument!");

        ConsumeAfterOpt = positionalOpts.get(0);
      }

      // Calculate how many positional values are _required_.
      boolean UnboundedFound = false;
      for (int i = ConsumeAfterOpt != null ? 1 : 0, e = positionalOpts
          .size(); i != e; ++i) {
        Option Opt = positionalOpts.get(i);
        if (RequiresValue(Opt))
          ++NumPositionalRequired;
        else if (ConsumeAfterOpt != null) {
          // ConsumeAfter cannot be combined with "optional" positional options
          // unless there is only one positional argument...
          if (positionalOpts.size() > 2)
            ErrorParsing |= Opt
                .error("error - this positional option will never be matched, " +
                    "because it does not Require a value, and a " +
                    "ConsumeAfter option is active!");
        } else if (UnboundedFound && !Opt.hasOptionName()) {
          // This option does not "require" a value...  Make sure this option is
          // not specified after an option that eats all extra arguments, or this
          // one will never get any!
          //
          ErrorParsing |= Opt
              .error("error - option can never match, because " +
                  "another positional argument will match an " +
                  "unbounded number of values, and this option" +
                  " does not require a value!");
        }
        UnboundedFound |= EatsUnboundedNumberOfValues(Opt);
      }
      HasUnlimitedPositionals = UnboundedFound || ConsumeAfterOpt != null;
    }

    // PositionalVals - A vector of "positional" arguments we accumulate into
    // the process at the end...
    //
    ArrayList<Pair<String, Integer>> PositionalVals = new ArrayList<>();

    // If the program has named positional arguments, and the asmName has been run
    // across, keep track of which positional argument was named.  Otherwise put
    // the positional args into the PositionalVals list...
    Option ActivePositionalArg = null;

    // Loop over all of the arguments... processing them.
    boolean DashDashFound = false;  // Have we read '--'?
    for (int i = 0; i < args.length; ++i) {
      Option Handler = null;
      String Value = null;
      String ArgName = "";

      // If the option list changed, this means that some command line
      // option has just been registered or deregistered.  This can occur in
      // response to things like -load, etc.  If this happens, rescan the options.
      if (optionListChanged) {
        positionalOpts.clear();
        sinkOpts.clear();
        optionsMap.clear();
        getOptionInfo(positionalOpts, sinkOpts, optionsMap);
        optionListChanged = false;
      }

      // Check to see if this is a positional argument.  This argument is
      // considered to be positional if it doesn't start with '-', if it is "-"
      // itself, or if we have seen "--" already.
      //
      if (args[i].charAt(0) != '-' || args[i].length() == 1
          || DashDashFound) {
        // Positional argument!
        if (ActivePositionalArg != null) {
          ProvidePositionalOption(ActivePositionalArg, args[i], i);
          continue;  // We are done!
        } else if (!positionalOpts.isEmpty()) {
          PositionalVals.add(new Pair<>(args[i], i));

          // All of the positional arguments have been fulfulled, give the rest to
          // the consume after option... if it's specified...
          //
          if (PositionalVals.size() >= NumPositionalRequired
              && ConsumeAfterOpt != null) {
            for (++i; i < args.length; ++i)
              PositionalVals.add(new Pair<>(args[i], i));
            break;   // Handle outside of the argument processing loop...
          }

          // Delay processing positional arguments until the end...
          continue;
        }
      } else if (args[i].equals("--") && !DashDashFound) {
        DashDashFound = true;  // This is the mythical "--"?
        continue;              // Don't try to process it as an argument itself.
      } else if (ActivePositionalArg != null && ActivePositionalArg.getMiscFlags()
          == PositionalEatsArgs) {
        // If there is a positional argument eating options, check to see if this
        // option is another positional argument.  If so, treat it as an argument,
        // otherwise feed it to the eating positional.
        ArgName = args[i] + 1;
        OutRef<String> x = new OutRef<>();
        Handler = lookupOption(ArgName, x, optionsMap);
        Value = x.get();
        if (Handler == null
            || Handler.getFormattingFlag() != Positional) {
          ProvidePositionalOption(ActivePositionalArg, args[i], i);
          continue;  // We are done!
        }

      } else {     // We start with a '-', must be an argument...
        ArgName = args[i].substring(1);
        OutRef<String> x = new OutRef<>();
        Handler = lookupOption(ArgName, x, optionsMap);
        Value = x.get();

        // Check to see if this "option" is really a prefixed or grouped argument.
        if (Handler == null) {
          String RealName = ArgName;
          if (RealName.length() > 1) {
            int length = 0;
            OutRef<Integer> len = new OutRef<>(length);
            Option PGOpt = getOptionPred(RealName, len,
                isPrefixedOrGrouping, optionsMap);

            length = len.get();
            // If the option is a prefixed option, then the value is simply the
            // rest of the asmName...  so fall through to later processing, by
            // setting up the argument asmName flags and value fields.
            //
            if (PGOpt != null && PGOpt.getFormattingFlag() == Prefix) {
              Value = ArgName.substring(length);
              String prefixName = ArgName.substring(0, length);
              Util.assertion(optionsMap.containsKey(prefixName) && optionsMap.get(prefixName) == PGOpt);

              Handler = PGOpt;
            } else if (PGOpt != null) {
              // This must be a grouped option... handle them now.
              Util.assertion(isGrouping.apply(PGOpt), "Broken getOptionPred!");

              do {
                // Move current arg asmName out of RealName into RealArgName...
                String RealArgName = RealName.substring(0, length);
                RealName = RealName.substring(length);

                // Because ValueRequired is an invalid flag for grouped arguments,
                // we don't need to pass args.length/args in...
                //
                Util.assertion(PGOpt.getValueExpectedFlag() != ValueRequired, "OptionInfo can not be Grouping AND ValueRequired!");

                OutRef<Integer> Dummy = new OutRef<>();
                ErrorParsing |= ProvideOption(PGOpt,
                    RealArgName, null, null, Dummy);

                // Get the next grouping option...
                PGOpt = getOptionPred(RealName, len,
                    isGrouping, optionsMap);
                length = len.get();
              } while (PGOpt != null && length != RealName.length());

              Handler = PGOpt; // Ate all of the options.
            }
          }
        }
      }

      if (Handler == null) {
        if (sinkOpts.isEmpty()) {
          System.err.println(": Undefined command line argument '"
              + args[i] + "'.  Try '" + ProgramName + " --help'");
          ErrorParsing = true;
        } else {
          for (Option opt : sinkOpts)
            opt.addOccurrence(i, "", args[i]);
        }
        continue;
      }

      // Check to see if this option accepts a comma separated list of values.  If
      // it does, we have to split up the value into multiple values...
      if (Value != null && Handler.getMiscFlags() == CommaSeparated) {
        String Val = Value;
        int Pos = Val.indexOf(',');
        while (Pos >= 0) {
          // Process the portion before the comma...
          OutRef<Integer> x = new OutRef<>(i);
          ErrorParsing |= ProvideOption(Handler, ArgName,
              Val.substring(0, Pos),
              args, x);
          i = x.get();

          // Erase the portion before the comma, AND the comma...
          Val = Val.substring(Pos + 1);
          Value = Value.substring(Pos + 1);
          // Check for another comma...
          Pos = Val.indexOf(',');
        }
      }

      // If this is a named positional argument, just remember that it is the
      // active one...
      if (Handler.getFormattingFlag() == Positional)
        ActivePositionalArg = Handler;
      else {
        OutRef<Integer> x = new OutRef<>(i);
        ErrorParsing |= ProvideOption(Handler, ArgName, Value, args, x);
        i = x.get();
      }
    }

    // Check and handle positional arguments now...
    if (NumPositionalRequired > PositionalVals.size()) {
      System.err
          .printf(": Not enough positional command line arguments specified!"
              + "Must specify at least" + String
              .valueOf(NumPositionalRequired)
              + " positional arguments: See: " + args[0]
              + " --help\n");

      ErrorParsing = true;
    } else if (!HasUnlimitedPositionals
        && PositionalVals.size() > positionalOpts.size()) {
      System.err.println(": Too many positional arguments specified!\n"
          + "Can specify at most " + positionalOpts.size()
          + " positional arguments: See: " + args[0] + " --help\n");
      ErrorParsing = true;

    } else if (ConsumeAfterOpt == null) {
      // Positional args have already been handled if ConsumeAfter is specified...
      int ValNo = 0, NumVals = PositionalVals.size();
      for (int i = 0, e = positionalOpts.size(); i != e; ++i) {
        if (RequiresValue(positionalOpts.get(i))) {
          ProvidePositionalOption(positionalOpts.get(i),
              PositionalVals.get(ValNo).first,
              PositionalVals.get(ValNo).second);
          ValNo++;
          --NumPositionalRequired;  // We fulfilled our duty...
        }

        // If we _can_ give this option more arguments, do so now, as long as we
        // do not give it values that others need.  'Done' controls whether the
        // option even _WANTS_ any more.
        //
        boolean Done = positionalOpts.get(i).getNumOccurrencesFlag()
            == Required;
        while (NumVals - ValNo > NumPositionalRequired && !Done) {
          switch (positionalOpts.get(i).getNumOccurrencesFlag()) {
            case Optional:
              Done = true;          // Optional arguments want _at most_ one value
              // FALL THROUGH
            case ZeroOrMore:    // Zero or more will take all they can get...
            case OneOrMore:     // One or more will take all they can get...
              ProvidePositionalOption(positionalOpts.get(i),
                  PositionalVals.get(ValNo).first,
                  PositionalVals.get(ValNo).second);
              ValNo++;
              break;
            default:
              Error.printFatalError("Internal error, unexpected NumOccurrences flag in " +
                  "positional argument processing!");
          }
        }
      }
    } else {
      Util.assertion((ConsumeAfterOpt != null && NumPositionalRequired <= PositionalVals.size()));

      int ValNo = 0;
      for (int j = 1, e = positionalOpts.size(); j != e; ++j)
        if (RequiresValue(positionalOpts.get(j))) {
          ErrorParsing |= ProvidePositionalOption(positionalOpts.get(j),
              PositionalVals.get(ValNo).first,
              PositionalVals.get(ValNo).second);
          ValNo++;
        }

      // Handle the case where there is just one positional option, and it's
      // optional.  In this case, we want to give JUST THE FIRST option to the
      // positional option and keep the rest for the consume after.  The above
      // loop would have assigned no values to positional options in this case.
      //
      if (positionalOpts.size() == 2 && ValNo == 0 && !PositionalVals
          .isEmpty()) {
        ErrorParsing |= ProvidePositionalOption(positionalOpts.get(0),
            PositionalVals.get(ValNo).first,
            PositionalVals.get(ValNo).second);
        ValNo++;
      }

      // Handle over all of the rest of the arguments to the
      // ConsumeAfter command line option...
      for (; ValNo != PositionalVals.size(); ++ValNo)
        ErrorParsing |= ProvidePositionalOption(ConsumeAfterOpt,
            PositionalVals.get(ValNo).first,
            PositionalVals.get(ValNo).second);
    }

    // Loop over args and make sure all required args are specified!
    for (Map.Entry<String, Option> pair : optionsMap.entrySet()) {
      switch (pair.getValue().getNumOccurrencesFlag()) {
        case Required:
        case OneOrMore:
          if (pair.getValue().getNumOccurrences() == 0) {
            pair.getValue()
                .error("must be specified at least once!");
            ErrorParsing = true;
          }
          // Fall through
        default:
          break;
      }
    }

    // Free all of the memory allocated to the map.  Command line options may only
    // be processed once!
    optionsMap.clear();
    positionalOpts.clear();

    // If we had an error processing our arguments, don't let the program execute
    if (ErrorParsing)
      System.exit(1);
  }

  /**
   * Scanning the registered option list for obtaining the positional option
   * and a map from option asmName to option object.
   *
   * @param positionalOpts
   * @param optionsMap
   */
  static void getOptionInfo(ArrayList<Option> positionalOpts,
                            ArrayList<Option> sinkOpts,
                            HashMap<String, Option> optionsMap) {
    ArrayList<String> optionNames = new ArrayList<>();
    // The ConsumeAfter option if it presents.
    Option<?> caOpt = null;

    for (Option opt = registeredOptionList;
         opt != null; opt = opt.getNextRegisteredOption()) {
      // If this option wants to handle multiple option names, get the full set.
      // This handles enum options like "-O1 -O2" etc.
      opt.getExtraOptionNames(optionNames);
      if (opt.hasOptionName())
        optionNames.add(opt.optionName);

      for (String name : optionNames) {
        // add argument to the argument map.
        if (optionsMap.keySet().contains(name)) {
          System.err.println(": CommandLine error: Argument '" + name
              + "' defined more than once!");
        } else {
          optionsMap.put(name, opt);
        }
      }

      optionNames.clear();

      // Remember information about positional options.
      if (opt.getFormattingFlag() == Positional)
        positionalOpts.add(opt);
      else if (opt.getMiscFlags() == Sink)
        sinkOpts.add(opt);
      else if (opt.getNumOccurrencesFlag() == ConsumeAfter) {
        if (caOpt != null)
          opt.error(
              "Cannot specify more than one option with ConsumeAfter!");
        caOpt = opt;
      }
    }

    if (caOpt != null)
      positionalOpts.add(caOpt);

    // Make sure that they are in order of registration not backwards.
    Collections.reverse(positionalOpts);
  }

  /// lookupOption - Lookup the option specified by the specified option on the
  /// command line.  If there is a value specified (after an equal sign) return
  /// that as well.
  static Option lookupOption(String arg,
                             OutRef<String> value,
                             HashMap<String, Option> optionsMap) {
    int i = 0;
    // Eat leading dashes
    while (arg.charAt(0) == '-')
      ++i;

    int begin = i, end = i;
    // Scan till end of argument asmName.
    while (end < arg.length() && arg.charAt(end) != '=')
      ++end;

    // If we have an equals sign...
    if (end < arg.length() && arg.charAt(end) == '=')
      // Get the value, not the equals
      value.set(arg.substring(end + 1));


    String optName = arg.substring(begin, end);
    // Look up the option.
    return optionsMap.containsKey(optName) ? optionsMap.get(optName) : null;
  }

  static boolean ProvideOption(Option Handler, String ArgName, String Value,
                               String[] args, OutRef<Integer> i) {
    // Is this a multi-argument option?
    int NumAdditionalVals = Handler.getNumAdditionalVals();

    // Enforce value requirements
    switch (Handler.getValueExpectedFlag()) {
      case ValueRequired:
        if (Value == null) {       // No value specified?
          if (i.get() + 1 < args.length) {     // Steal the next argument, like for '-o filename'
            i.set(i.get() + 1);
            Value = args[i.get()];
          } else {
            return Handler.error("requires a value!");
          }
        }
        break;
      case ValueDisallowed:
        if (NumAdditionalVals > 0)
          return Handler.error("multi-valued option specified" +
              " with ValueDisallowed modifier!");

        if (Value != null)
          return Handler.error("does not allow a value! '"
              + Value + "' specified.");
        break;
      case ValueOptional:
        break;
      default:
        System.err.println(": Bad ValueMask flag! CommandLine usage error:"
            + Handler.getValueExpectedFlag());
    }

    // If this isn't a multi-arg option, just run the handler.
    if (NumAdditionalVals == 0) {
      return Handler.addOccurrence(i.get(), ArgName, Value != null ? Value : "");
    }
    // If it is, run the handle several times.
    else {
      boolean MultiArg = false;

      if (Value != null) {
        if (Handler.addOccurrence(i.get(), ArgName, Value, MultiArg))
          return true;
        --NumAdditionalVals;
        MultiArg = true;
      }

      while (NumAdditionalVals > 0) {
        if (i.get() + 1 < args.length) {
          i.set(i.get() + 1);
          Value = args[i.get()];
        } else {
          return Handler.error("not enough values!");
        }
        if (Handler.addOccurrence(i.get(), ArgName, Value, MultiArg))
          return true;
        MultiArg = true;
        --NumAdditionalVals;
      }
      return false;
    }
  }

  static boolean ProvidePositionalOption(Option Handler, String Arg, int i) {
    return ProvideOption(Handler, Handler.optionName,
        Arg, null, new OutRef<>(i));
  }

  // OptionInfo predicates...
  static Function<Option, Boolean> isGrouping = option ->
  {
    return option.getFormattingFlag() == Grouping;
  };

  static Function<Option, Boolean> isPrefixedOrGrouping = option ->
  {
    return isGrouping.apply(option) || option.getFormattingFlag() == Prefix;
  };

  // getOptionPred - Check to see if there are any options that satisfy the
  // specified predicate with names that are the prefixes in asmName.  This is
  // checked by progressively stripping characters off of the asmName, checking to
  // see if there options that satisfy the predicate.  If we find one, return it,
  // otherwise return null.
  //
  static Option getOptionPred(String name, OutRef<Integer> length,
                              Function<Option, Boolean> Pred, HashMap<String, Option> OptionsMap) {
    if (OptionsMap.containsKey(name) && Pred.apply(OptionsMap.get(name))) {
      length.set(name.length());
      return OptionsMap.get(name);
    }
    if (name.length() == 1)
      return null;

    do {
      name = name.substring(0, name.length() - 1);   // Chop off the last character...

      // Loop while we haven't found an option and Name still has at least two
      // characters in it (so that the next iteration will not be the isEmpty
      // string...
    } while ((!OptionsMap.containsKey(name) || !Pred.apply(OptionsMap.get(name)))
        && name.length() > 1);

    if (OptionsMap.containsKey(name) && Pred.apply(OptionsMap.get(name))) {
      length.set(name.length());
      return OptionsMap.get(name);    // Found one!
    }
    return null;                // No option found!
  }

  static boolean RequiresValue(Option O) {
    return O.getNumOccurrencesFlag() == Required
        || O.getNumOccurrencesFlag() == OneOrMore;
  }

  static boolean EatsUnboundedNumberOfValues(Option O) {
    return O.getNumOccurrencesFlag() == ZeroOrMore
        || O.getNumOccurrencesFlag() == OneOrMore;
  }

  /**
   * --help and --help-hidden option implementation.
   */
  public static class HelpPrinter extends LocationClass<Boolean> {
    private int maxArgLen;
    private boolean showHidden;
    private Option emptyArg;

    /**
     * Predicates to be used to filtere down arg list.
     *
     * @param optPair
     * @return
     */
    private static Predicate<Pair<String, Option>> isHidden = optPair ->
    {
      OptionHidden oh = optPair.second.getOptionHiddenFlag();
      return oh != null && oh.value >= Hidden.value;
    };

    public static Predicate<Pair<String, Option>> isReallyHidden = optPair -> {
      OptionHidden oh = optPair.second.getOptionHiddenFlag();
      return oh != null && oh.value >= ReallyHidden.value;
    };

    public HelpPrinter(boolean showHidden) {
      this.showHidden = showHidden;
      emptyArg = null;
    }

    @Override
    public void setLocation(Boolean location) {
      if (!location)
        return;

      // Get all the options.
      ArrayList<Option> positionalOpts = new ArrayList<>();
      ArrayList<Option> sinkOpts = new ArrayList<>();
      HashMap<String, Option> optionsMap = new HashMap<>();

      // Process all registered options.
      getOptionInfo(positionalOpts, sinkOpts, optionsMap);

      // Copy all element in optionsMap to temps for sorting them.
      ArrayList<Pair<String, Option>> temps = new ArrayList<>();
      optionsMap.forEach((key, value) -> temps.add(Pair.get(key, value)));

      // Filter option with showHidden flag.
      List<Pair<String, Option>> opts = temps.stream().filter(stringOptionPair ->
      {
        return !(showHidden ? isReallyHidden : isHidden).test(stringOptionPair);
      }).collect(toList());

      // Eliminate duplicate entries in table.
      HashSet<Option> optionSet = new HashSet<>();
      opts.removeIf(strOpt -> !optionSet.add(strOpt.second));

      if (!(ProgramOverview == null || ProgramOverview.isEmpty()))
        out.printf("OVERVIEW: %s\n", ProgramOverview);

      out.printf("Usage: %s [options]", ProgramName);
      // Print out the positional options.

      Option caOpt = null;
      if (!positionalOpts.isEmpty() &&
          positionalOpts.get(0).getNumOccurrencesFlag() == ConsumeAfter) {
        caOpt = positionalOpts.get(0);
      }

      for (int i = (caOpt != null ? 1 : 0), e = positionalOpts.size(); i != e; i++) {
        Option opt = positionalOpts.get(i);
        if (opt.optionName != null)
          out.printf(" --%s", opt.optionName);
        out.printf(" %s", opt.helpStr);
      }

      // Print out the consume after option into console if it exists.
      if (caOpt != null)
        out.printf(" %s", caOpt.helpStr);

      out.println('\n');

      // Compute the maximum argument length.
      maxArgLen = 0;
      for (int i = 0, e = opts.size(); i != e; i++) {
        maxArgLen = Math.max(opts.get(i).second.getOptionWidth(), maxArgLen);
      }

      out.println("OPTIONS:");

      // Print out each options into console.
      // Sort the options table in alphabetic.
      opts.sort(Comparator.comparing(o -> o.first));

      for (int i = 0, e = opts.size(); i != e; i++) {
        opts.get(i).second.printOptionInfo(maxArgLen);
      }
      //opts.forEach(opt->opt.second.printOptionInfo(maxArgLen));

      // Halt the program since help information was printed out.
      System.exit(1);
    }
  }

  /**
   * Define two {@linkplain HelpPrinter} instances that are used to print out
   * help message out console, help-hidden.
   */
  public static HelpPrinter NormalPrinter = new HelpPrinter(false);
  public static HelpPrinter HiddenPrinter = new HelpPrinter(true);

  public static LocationOpt<Boolean> HOp =
      new LocationOpt<Boolean>(
          new ParserBool(),
          optionName("help"),
          desc("Display available options (--help-hidden for more)"),
          new LocationClassApplicator<Boolean>(NormalPrinter),
          new ValueExpectedApplicator(ValueDisallowed));

  public static LocationOpt<Boolean> HHOp =
      new LocationOpt<Boolean>(
          new ParserBool(),
          optionName("help-hidden"),
          desc("Display all available options"),
          new LocationClassApplicator<>(HiddenPrinter),
          new ValueExpectedApplicator(ValueDisallowed));

  /**
   * Printer for printing out version information about xcc compiler.
   */
  public static class VersionPrinter extends LocationClass<Boolean> {
    public void print() {
      out.println("Extremely C compiler");
      out.printf("  %s%n", Config.XCC_Full_Name);

      ArrayList<Pair<String, Target>> targets = new ArrayList<>();
      int width = 0;
      for (Iterator<Target> itr = TargetRegistry.iterator(); itr.hasNext(); ) {
        Target t = itr.next();
        targets.add(Pair.get(t.getName(), t));
        width = Math.max(width, t.getName().length());
      }

      // Sort the targets in the order of target name.
      targets.sort(Comparator.comparing(o -> o.first));

      out.println("  Registered Targets:");
      for (Pair<String, Target> pair : targets) {
        Target t = pair.second;
        out.printf("    %s%s - %s\n", t.getName(),
            Util.fixedLengthString(width - t.getName().length(), ' '),
            t.getShortDescription());
      }
      if (targets.isEmpty())
        out.println("    (none)");
    }

    @Override
    public void setLocation(Boolean optionWasSpecified) {
      if (optionWasSpecified) {
        print();
        System.exit(1);
      }
    }
  }

  /**
   * Define the --version option that printts out the xcc version for the tol.
   */
  public static VersionPrinter VersionPrinterInstance =
      new VersionPrinter();

  public static final LocationOpt<Boolean> VerOp =
      new LocationOpt<Boolean>(
          new ParserBool(),
          new OptionNameApplicator("version"),
          desc("Display the version of this program"),
          new LocationClassApplicator<>(VersionPrinterInstance),
          new ValueExpectedApplicator(ValueDisallowed));
}
