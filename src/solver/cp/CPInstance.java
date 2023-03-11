package solver.cp;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Random;
import java.util.Scanner;

import ilog.concert.IloException;
import ilog.concert.IloIntExpr;
import ilog.concert.IloIntVar;
import ilog.cp.IloCP;
import ilog.cp.IloIntValueEval;
import ilog.cp.IloSearchPhase;
import ilog.cp.IloValueSelector;
import ilog.cp.IloVarSelector;

public class CPInstance {
  // BUSINESS parameters
  int numWeeks;
  int numDays;
  int numEmployees;
  int numShifts;
  int numIntervalsInDay;
  int[][] minDemandDayShift;
  int minDailyOperation;

  // EMPLOYEE parameters
  int minConsecutiveWork;
  int maxDailyWork;
  int minWeeklyWork;
  int maxWeeklyWork;
  int maxConsecutiveNightShift;
  int maxTotalNightShift;

  IloCP cp;

  IloIntVar[][] shiftCodes;
  // The off shift is denoted by 0 while work shifts, night, day, and evening are
  // denoted by 1, 2, and 3 respectively.
  final int OFF_SHIFT = 0;
  final int NIGHT_SHIFT = 1;
  final int DAY_SHIFT = 2;
  final int EVENING_SHIFT = 3;

  IloIntVar[][] hoursWorked;

  int[] validWorkDurations = new int[] { 0, 4, 5, 6, 7, 8 };

  Random rand = new Random(System.nanoTime());

  public CPInstance(String fileName) {
    try {
      Scanner read = new Scanner(new File(fileName));

      while (read.hasNextLine()) {
        String line = read.nextLine();
        String[] values = line.split(" ");
        if (values[0].equals("Business_numWeeks:")) {
          numWeeks = Integer.parseInt(values[1]);
        } else if (values[0].equals("Business_numDays:")) {
          numDays = Integer.parseInt(values[1]);
        } else if (values[0].equals("Business_numEmployees:")) {
          numEmployees = Integer.parseInt(values[1]);
        } else if (values[0].equals("Business_numShifts:")) {
          numShifts = Integer.parseInt(values[1]);
        } else if (values[0].equals("Business_numIntervalsInDay:")) {
          numIntervalsInDay = Integer.parseInt(values[1]);
        } else if (values[0].equals("Business_minDemandDayShift:")) {
          int index = 1;
          minDemandDayShift = new int[numDays][numShifts];
          for (int d = 0; d < numDays; d++)
            for (int s = 0; s < numShifts; s++)
              minDemandDayShift[d][s] = Integer.parseInt(values[index++]);
        } else if (values[0].equals("Business_minDailyOperation:")) {
          minDailyOperation = Integer.parseInt(values[1]);
        } else if (values[0].equals("Employee_minConsecutiveWork:")) {
          minConsecutiveWork = Integer.parseInt(values[1]);
        } else if (values[0].equals("Employee_maxDailyWork:")) {
          maxDailyWork = Integer.parseInt(values[1]);
        } else if (values[0].equals("Employee_minWeeklyWork:")) {
          minWeeklyWork = Integer.parseInt(values[1]);
        } else if (values[0].equals("Employee_maxWeeklyWork:")) {
          maxWeeklyWork = Integer.parseInt(values[1]);
        } else if (values[0].equals("Employee_maxConsecutiveNigthShift:")) {
          maxConsecutiveNightShift = Integer.parseInt(values[1]);
        } else if (values[0].equals("Employee_maxTotalNigthShift:")) {
          maxTotalNightShift = Integer.parseInt(values[1]);
        }
      }
    } catch (FileNotFoundException e) {
      System.out.println("Error: file not found " + fileName);
    }
  }

  public void solve() {
    try {
      cp = new IloCP();

      hoursWorked = new IloIntVar[numEmployees][numDays];
      shiftCodes = new IloIntVar[numEmployees][numDays];

      // Implicit in structure:
      // Employees must start and finish on hour intervals
      // An employee can only be assigned to a single shift.

      for (int employee = 0; employee < numEmployees; employee++) {
        shiftCodes[employee] = cp.intVarArray(numDays, 0, 3);

        // employees cannot work more than 8 hours per day
        // employees to work at least 4 consecutive hours
        hoursWorked[employee] = cp.intVarArray(numDays, validWorkDurations, "");

        for (int day = 0; day < numDays; day++) {
          cp.add(cp.eq(
              cp.eq(shiftCodes[employee][day], OFF_SHIFT),
              cp.eq(hoursWorked[employee][day], 0)));
        }
      }

      // minDemandDayShift[0][2]=4 means that there needs to be at least 4 employees
      // working for the day shift on the first day.
      for (int day = 0; day < numDays; day++) {
        IloIntVar[] shiftsThisDay = new IloIntVar[numEmployees];
        for (int employee = 0; employee < numEmployees; employee++) {
          shiftsThisDay[employee] = shiftCodes[employee][day];
        }

        if (minDemandDayShift[day][OFF_SHIFT] > 0)
          cp.add(cp.ge(cp.count(shiftsThisDay, OFF_SHIFT), minDemandDayShift[day][0]));
        if (minDemandDayShift[day][NIGHT_SHIFT] > 0)
          cp.add(cp.ge(cp.count(shiftsThisDay, NIGHT_SHIFT), minDemandDayShift[day][1]));
        if (minDemandDayShift[day][DAY_SHIFT] > 0)
          cp.add(cp.ge(cp.count(shiftsThisDay, DAY_SHIFT), minDemandDayShift[day][2]));
        if (minDemandDayShift[day][EVENING_SHIFT] > 0)
          cp.add(cp.ge(cp.count(shiftsThisDay, EVENING_SHIFT), minDemandDayShift[day][3]));
      }

      // there is a minimum demand that needs to be met to ensure the daily operation
      // (minDailyOperation) for every day
      for (int day = 0; day < numDays; day++) {
        IloIntVar[] hoursWorkedThisDay = new IloIntVar[numEmployees];
        for (int employee = 0; employee < numEmployees; employee++) {
          hoursWorkedThisDay[employee] = hoursWorked[employee][day];
        }
        cp.add(cp.ge(cp.sum(hoursWorkedThisDay), minDailyOperation));
      }

      // In order to get employees up to speed with the manufacturing process, the rst
      // 4 days of the schedule is treated specially where employees are assigned to
      // unique shifts.
      for (int employee = 0; employee < numEmployees; employee++) {
        // I'm assuming that the number of days is always more than 3
        IloIntVar[] first4days = new IloIntVar[] {
            shiftCodes[employee][0], shiftCodes[employee][1],
            shiftCodes[employee][2], shiftCodes[employee][3]
        };
        cp.add(cp.allDiff(first4days));
      }

      // the total number of hours an employee works cannot exceed the standard
      // 40-hours per week and it should not be less than 20-hours
      // I'm assuming that the number of days is always number of weeks * 7
      for (int weekNumber = 0; weekNumber < numWeeks; weekNumber++) {
        for (int employee = 0; employee < numEmployees; employee++) {
          int offset = 7 * weekNumber;
          IloIntVar[] hoursWorkedThisWeek = new IloIntVar[] {
              hoursWorked[employee][offset + 0], hoursWorked[employee][offset + 1],
              hoursWorked[employee][offset + 2], hoursWorked[employee][offset + 3],
              hoursWorked[employee][offset + 4], hoursWorked[employee][offset + 5],
              hoursWorked[employee][offset + 6]
          };
          cp.add(cp.range(20, cp.sum(hoursWorkedThisWeek), 40));
        }
      }

      // It is known that night shifts are stressful, therefore night shifts cannot
      // follow each other (maxConsecutiveNightShift=1)
      // would have loved to use something like
      // https://sofdem.github.io/gccat/gccat/Cinterval_and_count.html#uid23715
      // but it doesn't look like I can do that in ILOG
      // Also, for the sake of simplicity, I'm going to *not* let this support any
      // other value other than maxConsecutiveNightShift=1
      for (int employee = 0; employee < numEmployees; employee++) {
        for (int day = 0; day < numDays - 1; day++) {
          cp.add(cp.imply(cp.eq(shiftCodes[employee][day], NIGHT_SHIFT),
              cp.neq(shiftCodes[employee][day + 1], NIGHT_SHIFT)));
        }
      }

      // there is a limit on the total number of night shifts that an employee can
      // perform (maxTotalNigthShift) across the scheduling horizon.
      for (int employee = 0; employee < numEmployees; employee++) {
        cp.add(cp.le(cp.count(shiftCodes[employee], NIGHT_SHIFT), maxTotalNightShift));
      }

      // Important: Do not change! Keep these parameters as is
      cp.setParameter(IloCP.IntParam.Workers, 1);
      cp.setParameter(IloCP.DoubleParam.TimeLimit, 300);

      cp.setParameter(IloCP.IntParam.LogVerbosity, IloCP.ParameterValues.Quiet);

      // We're not going to use IBM's intelligent proproetary search, but rather
      // out own modified dfs search (just to make it interesting!)
      cp.setParameter(IloCP.IntParam.SearchType, IloCP.ParameterValues.DepthFirst);
      String filenameSuffix = setSearchMethodToFastButBoring();

      addSymmetryBreaks();

      // Feel free to comment this part out if you're using a search method that
      // doesn't make use of randomization (you'll still have to make use of hasSolved
      // though)
      double failLimit = 45 * Math.round(2 * numEmployees);
      double failLimitMultiplier = 1.08;
      int runCount = 0;
      int limitIncreaseThreshold = 100 * numWeeks;
      cp.setParameter(IloCP.IntParam.FailLimit, (int) failLimit);
      boolean hasSolved = cp.solve();

      while (!hasSolved) {
        if (runCount % limitIncreaseThreshold == 0) {
          failLimit *= failLimitMultiplier;
          cp.setParameter(IloCP.IntParam.FailLimit, (int) failLimit);
          System.out.println("Restarting with restart limit: " +
              (int) failLimit + " for " + limitIncreaseThreshold + " runs.");
        }

        cp.setParameter(IloCP.IntParam.RandomSeed, rand.nextInt(20000000));
        hasSolved = cp.solve();
        runCount++;
      }

      if (hasSolved) {
        cp.printInformation();
        //IOHelper.generateVisualizerInput(numEmployees, numDays, this, filenameSuffix);
      } else {
        System.out.println("No Solution found!");
        System.out.println("Number of fails: " + cp.getInfo(IloCP.IntInfo.NumberOfFails));
      }
    } catch (IloException e) {
      System.out.println("Error: " + e);
    }
  }

  public String getEmployeeHours() {
    String hours = "";
    for (int employee = 0; employee < numEmployees; employee++) {
      for (int day = 0; day < numDays; day++) {
        int[] employeeStartAndEnd = getEmployeeStartAndEnd(employee, day);
        hours += String.format("%s %s ", employeeStartAndEnd[0], employeeStartAndEnd[1]);
      }
    }
    return hours.trim();
  }

  public int[] getEmployeeStartAndEnd(int employee, int day) {
    switch (cp.getIntValue(shiftCodes[employee][day])) {
      case 0:
        return new int[] { -1, -1 };
      case 1:
        return new int[] { 0, cp.getIntValue(hoursWorked[employee][day]) };
      case 2:
        return new int[] { 8, 8 + cp.getIntValue(hoursWorked[employee][day]) };
      default:
        return new int[] { 16, 16 + cp.getIntValue(hoursWorked[employee][day]) };
    }
  }

  void addSymmetryBreaks() throws IloException {
    // Sort them based on their first 2 days (there's enough entropy there to add
    // some structure...);
    // I originally used all 4 days but I think that was too specific and
    // led to a lot of backtracking...
    IloIntExpr[] first4Days = new IloIntExpr[numEmployees];
    for (int employee = 0; employee < numEmployees; employee++) {
      IloIntVar day1 = shiftCodes[employee][0];
      IloIntVar day2 = shiftCodes[employee][1];
      first4Days[employee] = cp.sum(cp.prod(1000, day1), cp.prod(100, day2));
    }

    for (int employee = 0; employee < numEmployees - 1; employee++) {
      cp.add(cp.le(first4Days[employee], first4Days[employee + 1]));
    }
  }

  /*****************************************************************
   * 
   * DIFFERENT SEARCH METHODS HERE
   *
   ****************************************************************/

  IloIntVar[] genericMatrixFlattener(IloIntVar[][] x) {
    IloIntVar[] x_flat = new IloIntVar[x[0].length * x.length];
    int index = 0;
    for (int i = 0; i < x.length; i++)
      for (int j = 0; j < x[0].length; j++)
        x_flat[index++] = x[i][j];
    return x_flat;
  }

  IloIntVar[] employeeBasedMatrixFlattenerByWeek(IloIntVar[][] x, int weekNumber) {
    IloIntVar[] x_flat = new IloIntVar[7 * numEmployees];
    int index = 0;
    for (int e = 0; e < numEmployees; e++)
      for (int d = weekNumber; d < weekNumber + 7; d++)
        x_flat[index++] = x[e][d];
    return x_flat;
  }

  String setSearchMethodToDefault() throws IloException {
    return "defaultSearch";
  }

  /**
   * This was intended to get people more regular schedules by biasing 50% of
   * employees to day and 50% to night
   * 
   * UPDATE: This one works as intended, I think.
   * People are having more regular schedules (after orientation),
   * but we also get a lot of slackers, where the first half take all the
   * off shifts and the rest have to take day shifts.
   * Really slow though, can't do instances with employees over 20 quickly...
   */
  String setSearchMethodDayEveningBiasFirstAttempt() throws IloException {
    // Let's try a 50/50 split, and let's see what happens if we prioritise all
    // shifts first, then make hours
    int employee;
    IloSearchPhase[] phases = new IloSearchPhase[numEmployees + 1];

    for (employee = 0; employee < shiftCodes.length / 2; employee++) {
      IloVarSelector[] varSel = new IloVarSelector[] { cp.selectSmallest(cp.domainSize()),
          cp.selectLargest(cp.varImpact()) };
      IloValueSelector valSel = cp.selectSmallest(cp.value()); // Bias towards day working
      phases[employee] = cp.searchPhase(shiftCodes[employee], cp.intVarChooser(varSel), cp.intValueChooser(valSel));
    }

    for (; employee < shiftCodes.length; employee++) {
      IloVarSelector[] varSel = new IloVarSelector[] { cp.selectSmallest(cp.domainSize()),
          cp.selectLargest(cp.varImpact()) };
      IloValueSelector valSel = cp.selectLargest(cp.value()); // Bias towards evening working
      phases[employee] = cp.searchPhase(shiftCodes[employee], cp.intVarChooser(varSel), cp.intValueChooser(valSel));
    }

    // Now that everyone's been given shifts, lets give people hours
    IloIntVar[] flattenedHours = genericMatrixFlattener(hoursWorked);
    phases[(phases.length) - 1] = cp.searchPhase(flattenedHours);

    cp.setSearchPhases(phases);
    return "DayEveningBiasFirstAttempt";
  }

  /**
   * This was intended to get improve on the problems of
   * setSearchMethodDayEveningBiasFirstAttempt
   * by setting the off shift to a random evaluation for every phase (so it will
   * be deprioritized randomly)
   * 
   * UPDATE: It is noticeably slower...
   * I think it's working, but minDemandDayShift gets in the way
   */
  String setSearchMethodDayEveningBiasRandomOff() throws IloException {
    int employee;
    IloSearchPhase[] phases = new IloSearchPhase[numEmployees + 1];

    for (employee = 0; employee < shiftCodes.length / 2; employee++) {
      IloVarSelector[] varSel = new IloVarSelector[] { cp.selectSmallest(cp.domainSize()),
          cp.selectSmallest(cp.varImpact()) };
      IloIntValueEval valueEvaluator = cp.explicitValueEval(
          new int[] { OFF_SHIFT, NIGHT_SHIFT, DAY_SHIFT, EVENING_SHIFT },
          new double[] { cp.getRandomNum() * 3, 1, 2, 1 }); // Bias towards day working
      IloValueSelector valSel = cp.selectLargest(valueEvaluator);
      phases[employee] = cp.searchPhase(shiftCodes[employee], cp.intVarChooser(varSel), cp.intValueChooser(valSel));
    }

    for (; employee < shiftCodes.length; employee++) {
      IloVarSelector[] varSel = new IloVarSelector[] { cp.selectSmallest(cp.domainSize()),
          cp.selectSmallest(cp.varImpact()) };
      IloIntValueEval valueEvaluator = cp.explicitValueEval(
          new int[] { OFF_SHIFT, NIGHT_SHIFT, DAY_SHIFT, EVENING_SHIFT },
          new double[] { cp.getRandomNum() * 3, 2, 1, 2 }); // Bias towards night working
      IloValueSelector valSel = cp.selectLargest(valueEvaluator);
      phases[employee] = cp.searchPhase(shiftCodes[employee], cp.intVarChooser(varSel), cp.intValueChooser(valSel));
    }

    // Now that everyone's been given shifts, lets give people hours
    IloIntVar[] flattenedHours = genericMatrixFlattener(hoursWorked);
    phases[(phases.length) - 1] = cp.searchPhase(flattenedHours);

    cp.setSearchPhases(phases);
    return "DayEveningBiasFirstAttemptRandomOff";
  }

  /**
   * What if we use IBS to make some employees more important than others?
   * Maybe that can be useful in terms of payroll.
   * UPDATE: This ends up making extremely regular schedules! It also did kind of
   * make a
   * subset of employees that took over day shifts for parts of the week (making
   * the
   * the "core" staff and everyone else the "support" staff). Just not in the way
   * I expected
   * (since the group of core staff changed during the week)
   * This is also nice because off shifts get distributed nicely
   * Pretty slow though.
   */
  String setSearchMethodCoreStaffSupportStaff() throws IloException {
    ArrayList<IloSearchPhase> phases = new ArrayList<>();

    for (int day = 0; day < numDays; day++) {
      IloIntVar[] workHoursThisDay = new IloIntVar[numEmployees];
      IloIntVar[] shiftsThisDay = new IloIntVar[numEmployees];
      for (int employee = 0; employee < numEmployees; employee++) {
        workHoursThisDay[employee] = hoursWorked[employee][day];
        shiftsThisDay[employee] = shiftCodes[employee][day];
      }

      // If we pick variables based on index but pick their values based on IBS, we
      // could get a hierarchy of "importance" (maybe!) based on their index...
      IloVarSelector[] varSelShifts = new IloVarSelector[] { cp.selectSmallest(cp.varIndex(shiftsThisDay)) };
      IloVarSelector[] varSelHours = new IloVarSelector[] { cp.selectSmallest(cp.varIndex(workHoursThisDay)) };
      IloValueSelector valSel = cp.selectLargest(cp.valueImpact());
      phases.add(cp.searchPhase(shiftsThisDay, cp.intVarChooser(varSelShifts), cp.intValueChooser(valSel)));
      phases.add(cp.searchPhase(workHoursThisDay, cp.intVarChooser(varSelHours), cp.intValueChooser(valSel)));
    }

    cp.setSearchPhases(phases.toArray(IloSearchPhase[]::new));
    return "CoreStaffSupportStaff";
  }

  /**
   * I wonder what would happen if I followed a similar approach to
   * setSearchMethodCoreStaffSupportStaff, but instead it also made it consider
   * off work shifts as the least-desired outcome...
   * UPDATE: doesn't seem that fundamentally differnet; it only shuffles things.
   */
  String setSearchMethodCoreStaffSupportStaffBadOffwork() throws IloException {
    ArrayList<IloSearchPhase> phases = new ArrayList<>();

    for (int day = 0; day < numDays; day++) {
      IloIntVar[] workHoursThisDay = new IloIntVar[numEmployees];
      IloIntVar[] shiftsThisDay = new IloIntVar[numEmployees];
      for (int employee = 0; employee < numEmployees; employee++) {
        workHoursThisDay[employee] = hoursWorked[employee][day];
        shiftsThisDay[employee] = shiftCodes[employee][day];
      }

      IloVarSelector[] varSelShifts = new IloVarSelector[] { cp.selectSmallest(cp.varIndex(shiftsThisDay)) };
      // Intentionally causing a tie so search tries everything else first before
      // offshift...
      IloIntValueEval shiftEvaluations = cp.explicitValueEval(
          new int[] { OFF_SHIFT, NIGHT_SHIFT, DAY_SHIFT, EVENING_SHIFT },
          new double[] { 1, 2, 2, 2 });

      IloValueSelector[] valSelShifts = new IloValueSelector[] {
          cp.selectLargest(shiftEvaluations),
          cp.selectLargest(cp.valueImpact())
      };

      IloVarSelector[] varSelHours = new IloVarSelector[] { cp.selectSmallest(cp.varIndex(workHoursThisDay)) };
      IloValueSelector valSelHours = cp.selectLargest(cp.valueImpact());

      phases.add(cp.searchPhase(shiftsThisDay, cp.intVarChooser(varSelShifts), cp.intValueChooser(valSelShifts)));
      phases.add(cp.searchPhase(workHoursThisDay, cp.intVarChooser(varSelHours), cp.intValueChooser(valSelHours)));
    }

    cp.setSearchPhases(phases.toArray(IloSearchPhase[]::new));
    return "CoreStaffSupportStaffBadOffwork";
  }

  /**
   * (┛◉Д◉) ┛彡┻━┻
   * I've just been informed that we're supposed to be using DFS search with all
   * our customizations.
   * That makes the search super slow, so I need something fast but boring to
   * compare things against.
   */
  String setSearchMethodToFastButBoring() throws IloException {
    ArrayList<IloSearchPhase> phases = new ArrayList<>();

    IloVarSelector[] varSel = new IloVarSelector[] {
        cp.selectSmallest(cp.domainSize()),
        cp.selectLargest(cp.domainMax()),
        cp.selectRandomVar()
    };
    IloValueSelector[] shiftValSel = new IloValueSelector[] {
        // Shift choices can have important effects downstream
        cp.selectLargest(cp.valueImpact()),
        // cp.selectLargest(shiftEvaluations),
        cp.selectRandomValue(),
    };
    IloValueSelector[] hourValSel = new IloValueSelector[] {
        cp.selectLargest(cp.value()) // Easier to start with the assumption that people can work as long as possible
    };

    phases.add(cp.searchPhase(
        genericMatrixFlattener(shiftCodes),
        cp.intVarChooser(varSel), cp.intValueChooser(shiftValSel)));
    phases.add(cp.searchPhase(
        genericMatrixFlattener(hoursWorked),
        cp.intVarChooser(varSel), cp.intValueChooser(hourValSel)));

    cp.setSearchPhases(phases.toArray(IloSearchPhase[]::new));
    return "FastButBoring";
  }
}
