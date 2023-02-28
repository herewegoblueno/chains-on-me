# This is designed to work on my local mac, not really the department machines

input="input/7_14.sched"
classfilefolder="compiled"

jarpath="/Applications/CPLEX_Studio_Community2211/cpoptimizer/lib/ILOG.CP.jar"
nativecode="/Applications/CPLEX_Studio_Community2211/opl/bin/x86-64_osx"

#Compile...
javac -cp $jarpath -d ./$classfilefolder ./src/solver/cp/*.java

#Run...
export LD_LIBRARY_PATH=$LD_LIBRARY_PATH:$nativecode1:$nativecode2:$nativecode3
#Can't use DYLD_LIBRARY_PATH because of Appple SIP (I think?); will use -Djava.library.path instead
#https://developer.apple.com/forums/thread/703757
java -Djava.library.path=$nativecode -cp $jarpath:$classfilefolder solver.cp.Main $input
