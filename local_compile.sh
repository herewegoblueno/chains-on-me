# This is designed to work on my local mac, not really the department machines

input="input/7_14.sched"

classfilefolder="compiled"
jarpath="/Applications/CPLEX_Studio2211/cpoptimizer/lib/ILOG.CP.jar"

#Compile...
javac -cp $jarpath -d ./$classfilefolder ./src/solver/cp/*.java