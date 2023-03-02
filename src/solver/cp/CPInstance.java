package solver.cp;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;

import ilog.concert.IloException;
import ilog.concert.IloIntExpr;
import ilog.concert.IloIntVar;
import ilog.cp.IloCP;

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

  // ILOG CP Solver
  IloCP cp;
  IloIntVar[][] startTimes;
  IloIntVar[][] endTimes;

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

      startTimes = new IloIntVar[numEmployees][numDays];
      endTimes = new IloIntVar[numEmployees][numDays];

      // The off shift is denoted by 0 while work shifts, night, day, and evening are
      // denoted by 1, 2, and 3 respectively.
      IloIntVar[][] shiftCodes = new IloIntVar[numEmployees][numDays];

      IloIntExpr[][] hoursWorked = new IloIntExpr[numEmployees][numDays];

      // Implicit in structure:
      // Employees must start and finish on hour intervals
      // An employee can only be assigned to a single shift.

      for (int employee = 0; employee < numEmployees; employee++) {
        startTimes[employee] = cp.intVarArray(numDays, -1, 24);
        endTimes[employee] = cp.intVarArray(numDays, -1, 24);

        // The off shift is denoted by 0 while work shifts, night, day
        // and evening are denoted by 1, 2, and 3 respectively.
        shiftCodes[employee] = cp.intVarArray(numDays, 0, 3);
        hoursWorked[employee] = cp.intExprArray(numDays);

        for (int day = 0; day < numDays; day++) {

          // employees cannot work more than 8 hours per day
          // employees to work at least 4 consecutive hours
          hoursWorked[employee][day] = cp.diff(endTimes[employee][day], startTimes[employee][day]);

          cp.add(cp.imply(
              cp.neq(startTimes[employee][day], -1),
              cp.range(minConsecutiveWork, hoursWorked[employee][day], maxDailyWork)));

          // Offwork shift is -1
          cp.add(cp.imply(
              cp.eq(shiftCodes[employee][day], 0),
              cp.and(cp.eq(endTimes[employee][day], -1), cp.eq(startTimes[employee][day], -1))));

          // night shift covers [00:00..08:00)
          cp.add(cp.imply(
              cp.eq(shiftCodes[employee][day], 1),
              cp.and(cp.range(0, endTimes[employee][day], 8), cp.range(0, startTimes[employee][day], 8))));

          // day shift covers [08:00..16:00)
          cp.add(cp.imply(
              cp.eq(shiftCodes[employee][day], 2),
              cp.and(cp.range(8, endTimes[employee][day], 16), cp.range(8, startTimes[employee][day], 16))));

          // evening shift covers [16:00-24:00).
          cp.add(cp.imply(
              cp.eq(shiftCodes[employee][day], 3),
              cp.and(cp.range(16, endTimes[employee][day], 24), cp.range(16, startTimes[employee][day], 24))));
        }
      }

      // minDemandDayShift[0][2]=4 means that there needs to be at least 4 employees
      // working for the day shift on the first day.
      for (int day = 0; day < numDays; day++) {
        IloIntVar[] shiftsThisDay = new IloIntVar[numEmployees];
        for (int employee = 0; employee < numEmployees; employee++) {
          shiftsThisDay[employee] = shiftCodes[employee][day];
        }

        if (minDemandDayShift[day][0] > 0)
          cp.add(cp.ge(cp.count(shiftsThisDay, 0), minDemandDayShift[day][0]));
        if (minDemandDayShift[day][1] > 0)
          cp.add(cp.ge(cp.count(shiftsThisDay, 1), minDemandDayShift[day][1]));
        if (minDemandDayShift[day][2] > 0)
          cp.add(cp.ge(cp.count(shiftsThisDay, 2), minDemandDayShift[day][2]));
        if (minDemandDayShift[day][3] > 0)
          cp.add(cp.ge(cp.count(shiftsThisDay, 3), minDemandDayShift[day][3]));
      }

      // there is a minimum demand that needs to be met to ensure the daily operation
      // (minDailyOperation) for every day
      for (int day = 0; day < numDays; day++) {
        IloIntExpr[] hoursWorkedThisDay = new IloIntExpr[numEmployees];
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
          IloIntExpr[] hoursWorkedThisWeek = new IloIntExpr[] {
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
      // but it doesn't look like I can do that in cplex
      // Also, for the sake of simplicity, I'm going to *not* let this support any
      // other value other than maxConsecutiveNightShift=1
      for (int employee = 0; employee < numEmployees; employee++) {
        for (int day = 0; day < numDays - 1; day++) {
          cp.add(cp.imply(cp.eq(shiftCodes[employee][day], 1), cp.neq(shiftCodes[employee][day + 1], 1)));
        }
      }

      // there is a limit on the total number of night shifts that an employee can
      // perform (maxTotalNigthShift) across the scheduling horizon.
      for (int employee = 0; employee < numEmployees; employee++) {
        cp.add(cp.le(cp.count(shiftCodes[employee], 1), maxTotalNightShift));
      }

      // Important: Do not change! Keep these parameters as is
      cp.setParameter(IloCP.IntParam.Workers, 1);
      cp.setParameter(IloCP.DoubleParam.TimeLimit, 300);
      // cp.setParameter(IloCP.IntParam.SearchType, IloCP.ParameterValues.DepthFirst);

      // Uncomment this: to set the solver output level if you wish
      // cp.setParameter(IloCP.IntParam.LogVerbosity, IloCP.ParameterValues.Quiet);

      if (cp.solve()) {
        cp.printInformation();
        //IOHelper.generateVisualizerInput(numEmployees, numDays, startTimes, endTimes, cp);
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
        hours += String.format("%s %s ",
            cp.getIntValue(startTimes[employee][day]),
            cp.getIntValue(endTimes[employee][day]));
      }
    }
    return hours.trim();
  }

}
