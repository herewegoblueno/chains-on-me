package solver.cp.examples;

import ilog.concert.IloException;
import ilog.concert.IloIntExpr;
import ilog.concert.IloIntVar;
import ilog.cp.IloCP;

public class ExampleSolver {

  // ILOG CP Solver
  IloCP cp;

  // SK: technically speaking, the model with the global constaints
  // should result in fewer number of fails. In this case, the problem
  // is so simple that, the solver is able to re-transform the model
  // and replace inequalities with the global all different constrains.
  // Therefore, the results don't really differ
  void solveAustraliaGlobal() {
    String[] Colors = { "red", "green", "blue" };
    try {
      cp = new IloCP();
      IloIntVar WesternAustralia = cp.intVar(0, 3);
      IloIntVar NorthernTerritory = cp.intVar(0, 3);
      IloIntVar SouthAustralia = cp.intVar(0, 3);
      IloIntVar Queensland = cp.intVar(0, 3);
      IloIntVar NewSouthWales = cp.intVar(0, 3);
      IloIntVar Victoria = cp.intVar(0, 3);

      IloIntExpr[] clique1 = new IloIntExpr[3];
      clique1[0] = WesternAustralia;
      clique1[1] = NorthernTerritory;
      clique1[2] = SouthAustralia;

      IloIntExpr[] clique2 = new IloIntExpr[3];
      clique2[0] = Queensland;
      clique2[1] = NorthernTerritory;
      clique2[2] = SouthAustralia;

      IloIntExpr[] clique3 = new IloIntExpr[3];
      clique3[0] = Queensland;
      clique3[1] = NewSouthWales;
      clique3[2] = SouthAustralia;

      IloIntExpr[] clique4 = new IloIntExpr[3];
      clique4[0] = Queensland;
      clique4[1] = Victoria;
      clique4[2] = SouthAustralia;

      cp.add(cp.allDiff(clique1));
      cp.add(cp.allDiff(clique2));
      cp.add(cp.allDiff(clique3));
      cp.add(cp.allDiff(clique4));

      cp.setParameter(IloCP.IntParam.Workers, 1);
      cp.setParameter(IloCP.DoubleParam.TimeLimit, 300);
      cp.setParameter(IloCP.IntParam.SearchType, IloCP.ParameterValues.DepthFirst);

      if (cp.solve()) {
        System.out.println();
        System.out.println("WesternAustralia:    " + Colors[(int) cp.getValue(WesternAustralia)]);
        System.out.println("NorthernTerritory:   " + Colors[(int) cp.getValue(NorthernTerritory)]);
        System.out.println("SouthAustralia:      " + Colors[(int) cp.getValue(SouthAustralia)]);
        System.out.println("Queensland:          " + Colors[(int) cp.getValue(Queensland)]);
        System.out.println("NewSouthWales:       " + Colors[(int) cp.getValue(NewSouthWales)]);
        System.out.println("Victoria:            " + Colors[(int) cp.getValue(Victoria)]);
      } else {
        System.out.println("No Solution found!");
      }
    } catch (IloException e) {
      System.out.println("Error: " + e);
    }
  }

  void solveAustraliaBinary() {
    String[] Colors = { "red", "green", "blue" };
    try {
      cp = new IloCP();
      IloIntVar WesternAustralia = cp.intVar(0, 3);
      IloIntVar NorthernTerritory = cp.intVar(0, 3);
      IloIntVar SouthAustralia = cp.intVar(0, 3);
      IloIntVar Queensland = cp.intVar(0, 3);
      IloIntVar NewSouthWales = cp.intVar(0, 3);
      IloIntVar Victoria = cp.intVar(0, 3);

      cp.add(cp.neq(WesternAustralia, NorthernTerritory));
      cp.add(cp.neq(WesternAustralia, SouthAustralia));
      cp.add(cp.neq(NorthernTerritory, SouthAustralia));
      cp.add(cp.neq(NorthernTerritory, Queensland));
      cp.add(cp.neq(SouthAustralia, Queensland));
      cp.add(cp.neq(SouthAustralia, NewSouthWales));
      cp.add(cp.neq(SouthAustralia, Victoria));
      cp.add(cp.neq(Queensland, NewSouthWales));
      cp.add(cp.neq(NewSouthWales, Victoria));

      cp.setParameter(IloCP.IntParam.Workers, 1);
      cp.setParameter(IloCP.DoubleParam.TimeLimit, 300);
      cp.setParameter(IloCP.IntParam.SearchType, IloCP.ParameterValues.DepthFirst);

      if (cp.solve()) {
        System.out.println();
        System.out.println("WesternAustralia:    " + Colors[(int) cp.getValue(WesternAustralia)]);
        System.out.println("NorthernTerritory:   " + Colors[(int) cp.getValue(NorthernTerritory)]);
        System.out.println("SouthAustralia:      " + Colors[(int) cp.getValue(SouthAustralia)]);
        System.out.println("Queensland:          " + Colors[(int) cp.getValue(Queensland)]);
        System.out.println("NewSouthWales:       " + Colors[(int) cp.getValue(NewSouthWales)]);
        System.out.println("Victoria:            " + Colors[(int) cp.getValue(Victoria)]);
      } else {
        System.out.println("No Solution found!");
      }
    } catch (IloException e) {
      System.out.println("Error: " + e);
    }
  }

  void solveSendMoreMoney() {
    try {
      // CP Solver
      cp = new IloCP();

      // SEND MORE MONEY
      IloIntVar S = cp.intVar(1, 9);
      IloIntVar E = cp.intVar(0, 9);
      IloIntVar N = cp.intVar(0, 9);
      IloIntVar D = cp.intVar(0, 9);
      IloIntVar M = cp.intVar(1, 9);
      IloIntVar O = cp.intVar(0, 9);
      IloIntVar R = cp.intVar(0, 9);
      IloIntVar Y = cp.intVar(0, 9);

      IloIntVar[] vars = new IloIntVar[] { S, E, N, D, M, O, R, Y };
      cp.add(cp.allDiff(vars));

      // 1000 * S + 100 * E + 10 * N + D
      // + 1000 * M + 100 * O + 10 * R + E
      // = 10000 * M + 1000 * O + 100 * N + 10 * E + Y

      IloIntExpr SEND = cp.sum(cp.prod(1000, S), cp.sum(cp.prod(100, E), cp.sum(cp.prod(10, N), D)));
      IloIntExpr MORE = cp.sum(cp.prod(1000, M), cp.sum(cp.prod(100, O), cp.sum(cp.prod(10, R), E)));
      IloIntExpr MONEY = cp.sum(cp.prod(10000, M),
          cp.sum(cp.prod(1000, O), cp.sum(cp.prod(100, N), cp.sum(cp.prod(10, E), Y))));

      cp.add(cp.eq(MONEY, cp.sum(SEND, MORE)));

      // Solver parameters
      cp.setParameter(IloCP.IntParam.Workers, 1);
      cp.setParameter(IloCP.IntParam.SearchType, IloCP.ParameterValues.DepthFirst);
      if (cp.solve()) {
        System.out.println(
            "  " + cp.getValue(S) + " " + cp.getValue(E) + " " + cp.getValue(N) + " " + cp.getValue(D));
        System.out.println(
            "  " + cp.getValue(M) + " " + cp.getValue(O) + " " + cp.getValue(R) + " " + cp.getValue(E));
        System.out.println(
            cp.getValue(M) + " " + cp.getValue(O) + " " + cp.getValue(N) + " " + cp.getValue(E) + " "
                + cp.getValue(Y));
      } else {
        System.out.println("No Solution!");
      }
    } catch (IloException e) {
      System.out.println("Error: " + e);
    }
  }

}
