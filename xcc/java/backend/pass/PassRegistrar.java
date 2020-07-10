package backend.pass;
/*
 * Extremely Compiler Collection
 * Copyright (c) 2015-2020, Jianping Zeng
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

import backend.passManaging.PassRegistrationListener;
import tools.Util;

import java.util.ArrayList;
import java.util.HashMap;

public final class PassRegistrar {
  /**
   * Mapping from Class information to corresponding PassInfo instance.
   * Instantiated by class {@linkplain RegisterPass}.
   */
  private static final HashMap<Class<? extends Pass>, PassInfo> passInfoMap
      = new HashMap<>();

  private static final HashMap<PassInfo, Class<? extends Pass>> registeredPasses
      = new HashMap<>();

  /**
   * All of pass registeration listener registered in here.
   */
  public static final ArrayList<PassRegistrationListener> listeners
      = new ArrayList<>();

  /**
   * A registeration interface to client.
   *
   * @param pass
   * @param pi
   */
  public static void registerPass(Class<? extends Pass> pass, PassInfo pi) {
    if (Util.DEBUG) {
      if (registeredPasses.containsKey(pi) || passInfoMap
          .containsKey(pass)) {
        pi.dump();
        Util.assertion(false, "Pass already registered!");
      }
    } else {
      Util.assertion(!registeredPasses.containsKey(pi) && !passInfoMap.containsKey(pass), "Pass already registered!");

    }
    registeredPasses.put(pi, pass);
    passInfoMap.put(pass, pi);

    if (!listeners.isEmpty()) {
      for (PassRegistrationListener listener : listeners) {
        listener.passRegistered(pi);
      }
    }
  }

  /**
   * This keeps track of which passes implement the interfaces
   * that are required by the current pass (to implement getAnalysisToUpDate()).
   */
  //public ArrayList<Pair<PassInfo, Pass>> analysisImpls;
  public static PassInfo getPassInfo(Class<? extends Pass> klass) {
    return lookupPassInfo(klass);
  }

  public static PassInfo lookupPassInfo(Class<? extends Pass> analysisClass) {
    if (passInfoMap == null) return null;
    PassInfo res = passInfoMap.get(analysisClass);
    if (passInfoMap.containsKey(analysisClass))
      return res;
    return null;
  }

  public void unregisterPass(PassInfo pi) {
    Util.assertion(pi != null && registeredPasses != null, "Pass register factory is uninstantiated as yet!");

    Util.assertion(registeredPasses.containsKey(pi), "Pass registered but not in register factory!");

    passInfoMap.remove(registeredPasses.remove(pi));
  }

  public static Pass getAnalysisOrNull(PassInfo passInfo) {
    if (passInfo == null)
      return null;
    return passInfo.createPass();
  }

  /**
   * Return the analysis result which must be existed.
   *
   * @param pi
   * @return
   */
  public static Pass getAnalysis(PassInfo pi) {
    Pass res = getAnalysisOrNull(pi);
    Util.assertion(res != null, "Pass has an incorrect pass used yet!");
    return res;
  }

  /**
   * Return an analysis pass or null if it is not existed.
   *
   * @param passInfo
   * @return
   */
  public static Pass getAnalysisToUpdate(PassInfo passInfo) {
    return getAnalysisOrNull(passInfo);
  }

  /**
   * Inform the pass listener to enumerate all of registered passes in here
   * with specified method.
   *
   * @param listener
   */
  public static void enumerateWith(PassRegistrationListener listener) {
    passInfoMap.values().forEach(listener::passEnumerate);
  }
}
