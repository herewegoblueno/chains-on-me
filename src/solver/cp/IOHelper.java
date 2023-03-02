package solver.cp;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import ilog.concert.IloIntVar;
import ilog.cp.IloCP;

public class IOHelper {

  /**
   * Generate Visualizer Input
   * author: lmayo1
   *
   * Generates an input solution file for the visualizer.
   * (https://cs.brown.edu/courses/csci2951-o/p2vis.html)
   * The file name is numDays_numEmployees_sol.txt
   * The file will be overwritten if it already exists.
   * 
   * @param numEmployees the number of employees
   * @param numDays      the number of days
   * @param beginED      int[e][d] the hour employee e begins work on day d, -1 if
   *                     not working
   * @param endED        int[e][d] the hour employee e ends work on day d, -1 if
   *                     not working
   */
  public static void generateVisualizerInput(int numEmployees, int numDays, IloIntVar[][] beginED, IloIntVar[][] endED, IloCP solver) {
    String solString = String.format("%d %d %n", numDays, numEmployees);

    for (int d = 0; d < numDays; d++) {
      for (int e = 0; e < numEmployees; e++) {
        solString += String.format("%d %d %n", (int) solver.getValue(beginED[e][d]), (int) solver.getValue(endED[e][d]));
      }
    }

    String fileName = Integer.toString(numDays) + "_" + Integer.toString(numEmployees) + "_sol.txt";

    try {
      File resultsFile = new File(fileName);
      if (resultsFile.createNewFile()) {
        System.out.println("File created: " + fileName);
      } else {
        System.out.println("Overwriting the existing " + fileName);
      }
      FileWriter writer = new FileWriter(resultsFile, false);
      writer.write(solString);
      writer.close();
    } catch (IOException e) {
      System.out.println("An error occurred.");
      e.printStackTrace();
    }
  }

  /**
   * Poor man's Gantt chart.
   * author: skadiogl
   *
   * Displays the employee schedules on the command line.
   * Each row corresponds to a single employee.
   * A "+" refers to a working hour and "." means no work
   * The shifts are separated with a "|"
   * The days are separated with "||"
   * 
   * This might help you analyze your solutions.
   * 
   * @param numEmployees the number of employees
   * @param numDays      the number of days
   * @param beginED      int[e][d] the hour employee e begins work on day d, -1 if
   *                     not working
   * @param endED        int[e][d] the hour employee e ends work on day d, -1 if
   *                     not working
   */
  public static void prettyPrintGanttChart(int numEmployees, int numDays, IloIntVar[][] beginED, IloIntVar[][] endED, int numIntervalsInDay, IloCP solver) {
    for (int e = 0; e < numEmployees; e++) {
      System.out.print("E" + (e + 1) + ": ");
      if (e < 9)
        System.out.print(" ");
      for (int d = 0; d < numDays; d++) {
        for (int i = 0; i < numIntervalsInDay; i++) {
          if (i % 8 == 0)
            System.out.print("|");
          if (beginED[e][d] != endED[e][d] && i >= (int)solver.getValue(beginED[e][d]) && i < (int)solver.getValue(endED[e][d]))
            System.out.print("+");
          else
            System.out.print(".");
        }
        System.out.print("|");
      }
      System.out.println(" ");
    }
  }
}