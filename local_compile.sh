# This is designed to work on my local mac, not really the department machines

classfilefolder="compiled"
jarpath="/Applications/CPLEX_Studio2211/cpoptimizer/lib/ILOG.CP.jar"

#Compile...
javac -cp $jarpath -d ./$classfilefolder ./src/solver/cp/*.java