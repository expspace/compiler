import java.util.ArrayList;


public class Compiler {
	
	public static boolean firstPass = true;
	
	public static void run(String input) {
		
		/* FIRST PASS PARSE: 
		 * produces token sequence, derivation sequence
		 * builds symbol tables, inserts semantic records for declarations
		 * records lexical errors, syntax errors, semantic errors
		 * semantic verifications: multiple declaration
		 */
		
		LexicalAnalyzer.setInputString(input);	
		FirstPassParser.parse();
		firstPass = false;

		/* SECOND PASS PARSE: 
		 * produces moon output
		 * uses symbol table, attribute migration with parameter passing
		 * records semantic errors
		 * semantic verification : undeclared variable, undefined data type/member, argument size/type match, 
		 * 		type match (expressions, return expressions, assignment, dimension match
		 * code generation : memory allocation (int, float, arrays), loop statement, conditional statement, input/output
		 * 		arithmetic/relational expressions, function declaration, function call, parameter passing
		 */
		LexicalAnalyzer.resetLexicalAnalyzer();
		CodeGenerator.generateUniqueAddresses();
		CodeGenerator.updateTempAddress();
		CodeGenerator.computeMemorySizes();
		
		if(LexicalAnalyzer.lexicalParseSuccess && FirstPassParser.syntaxParseSuccess && SemanticAnalyzer.semanticParseSuccess) {
			SecondPassParser.parse();	
		}
		
		printExecutionInfo();
	}
	
	
	public static void printExecutionInfo() {
		
		//add lexical error reporting lexicalErrorList
		if(FirstPassParser.syntaxParseSuccess && SemanticAnalyzer.semanticParseSuccess) {
			System.out.println("PARSE SUCCESSFUL");
			System.out.println("================");
			System.out.println("Zero errors detected.");
			System.out.println();
		} else {
			System.out.println("PARSE UNSUCCESSFUL");
			System.out.println("==================");
			
			System.out.println("Lexical Errors");
			System.out.println("--------------");
			if(!LexicalAnalyzer.lexicalParseSuccess) {
				System.out.println(LexicalAnalyzer.lexicalErrorCount + " error(s) detected.");
				System.out.println();
				printErrorList(LexicalAnalyzer.lexicalErrorList);
			} else {
				System.out.println("Zero errors detected.");
				System.out.println();
			}
			
			System.out.println("Syntax Errors");
			System.out.println("-------------");
			if(!FirstPassParser.syntaxParseSuccess) {
				System.out.println(FirstPassParser.syntaxErrorCount + " error(s) detected.");
				System.out.println();
				printErrorList(FirstPassParser.syntaxErrorList);
			} else {
				System.out.println("Zero errors detected.");
				System.out.println();
			}
			
			System.out.println("Semantic Errors");
			System.out.println("---------------");
			if(!SemanticAnalyzer.semanticParseSuccess) {
				System.out.println(SemanticAnalyzer.semanticErrorCount + " error(s) detected.");
				System.out.println();
				printErrorList(SemanticAnalyzer.semanticErrorList);
			} else {
				System.out.println("Zero errors detected.");
				System.out.println();
			}
		}	
		
	}
	
	public static void printErrorList(ArrayList<String> errorList) {
		for(String error : errorList) {
			System.out.println(error);
			System.out.println();
		}	
	}
}
