# Compiler Project 
A semester long project for a course on compiler design. The compiler translates from a source language similar in syntax to the C programming language to MOON assembly code which runs on the MOON virtual machine. While compiling the program checks for syntactic as well as semantic and lexicographical errors and outputs detailed error messages if found. This project includes the following components used in the compilation process::

  * Lexical Analysis
  * Parsing (Syntactical Analysis)
  * Symbol Tables 
  * Semantic Analysis
  * Code Generation

Files to compile are located in the folder "InputFiles". After running the program a prompt for the file name occurs, 
write the file name as found in the "InputFiles" folder. Errors detected are displayed on the console window and output
for token sequence, derivation sequence, symbol table info and moon output are located in the folder "OutputFiles".

Known Missing Features/Bugs:
  * Code Generation : missing offset mechanism (arrays and class variables)
  * Code Generation : missing class memory size computation 
  * Code Generation : floating point number support
  * Semantic Analysis : nested variables attribute migration bugs (type, method calls)
