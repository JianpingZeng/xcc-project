package backend.analysis;

import tools.Util;

/**
 * This class defines a simple visitor class that may be used for various
 * SCEV analysis purposes.
 * @author Xlous.zeng
 * @version 0.1
 */
public interface SCEVVisitor<RetVal>
{
	default RetVal visit(SCEV s)
	{
		switch (s.getSCEVType())
		{
			case scConstant:
				return visitConstant((SCEVConstant)s);
			case scAddExpr:
				return visitAddExpr((SCEVAddExpr)s);
			case scMulExpr:
				return visitMulExpr((SCEVMulExpr)s);
			case scSDivExpr:
				return visitSDivExpr((SCEVSDivExpr)s);
			case scAddRecExpr:
				return visitAddRecExpr((SCEVAddRecExpr)s);
			case scUnknown:
				return visitUnknown((SCEVUnknown)s);
			case scCouldNotCompute:
				return visitCouldNotCompute((SCEVCouldNotCompute)s);
			default:
				Util.shouldNotReachHere("Invalid use of SCEVCouldNotCompute");
				return (RetVal) new Object();
		}
	}

	RetVal visitConstant(SCEVConstant s);

	RetVal visitAddExpr(SCEVAddExpr s);

	RetVal visitMulExpr(SCEVMulExpr s);

	RetVal visitSDivExpr(SCEVSDivExpr s);

	RetVal visitAddRecExpr(SCEVAddRecExpr s);

	RetVal visitUnknown(SCEVUnknown s);

	default RetVal visitCouldNotCompute(SCEVCouldNotCompute s)
	{
		Util.shouldNotReachHere("Invalid use of SCEVCouldNotCompute");
		return (RetVal) new Object();
	}
}
