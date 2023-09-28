//package backend.solvers.SMTLib
//
//import org.scalatest.funsuite.AnyFunSuite
//
//class SmtLibTest extends AnyFunSuite() {
//
//  test("experimenting") {
//
//    val smt = new SMT
//    val efactory = smt.smtConfig.exprFactory
//    val sortfactory = smt.smtConfig.sortFactory
//    val solver: ISolver =
//      new Solver_z3_4_3(smt.smtConfig,
//                        "/Users/mvdcamme/PhD/Projects/Concolic_Execution/z3/build/z3")
//    solver.start
//    solver.set_option(efactory.keyword(":produce-models"), efactory.symbol("true"))
//    val e: IExpr = efactory.symbol("(= i1 10)")
//    val intsort = sortfactory.createSortExpression(efactory.symbol("Int"), new ISort(0))
//    solver.declare_fun(new C_declare_fun(efactory.symbol("i0"), List(), intsort))
//    solver.assertExpr(e)
//    var r = solver.set_logic("QF_UF", null.asInstanceOf[IPos])
//    r = solver.check_sat
//    r = solver.get_value(efactory.symbol("i1"))
//    assert(true)
//
////    val bc: BranchConstraint = BranchConstraint(RelationalSymbolicExpression(SymbolicInput(0, 1), IntEqual, SymbolicInt(10)))
//  }
//
//}
