import java.util.ArrayList;
import java.util.HashMap;


public class CodeGenerator {
	
	public static ArrayList<String> moonInstructionList = new ArrayList<String>();
	
	public static final int INT_MEMORY_SIZE = 4;	//#bytes
	public static final int FLOAT_MEMORY_SIZE = 8;	//#bytes
	
	public static String tempAddress; //used for intermediate results
	public static int uniqueNumber; //used for assigning unique memory addresses
	
	public static HashMap<TokenType, String> moonOpMap = new HashMap<TokenType, String>(); //add "i" for immediate operand operations
		static {
			moonOpMap.put(TokenType.ADD, 		  "add"); 
			moonOpMap.put(TokenType.SUBTRACT, 	  "sub"); 
			moonOpMap.put(TokenType.MULTIPLY, 	  "mul");
			moonOpMap.put(TokenType.DIVIDE, 	  "div"); 
			moonOpMap.put(TokenType.AND, 		  "and"); 
			moonOpMap.put(TokenType.OR, 		  "or");  
			moonOpMap.put(TokenType.NOT, 		  "not");
			moonOpMap.put(TokenType.EQUAL, 		  "ceq"); 
			moonOpMap.put(TokenType.NOTEQUAL, 	  "cne"); 
			moonOpMap.put(TokenType.LESSTHAN, 	  "clt"); 
			moonOpMap.put(TokenType.LESSTHANOREQ, "cle"); 
			moonOpMap.put(TokenType.GREATTHAN, 	  "cgt"); 
			moonOpMap.put(TokenType.GREATTHANOREQ,"cge"); 
		}
		
	public static void generateUniqueAddresses() {
		for(SymbolTable symbolTable : SemanticAnalyzer.symbolTableList) {
			for(SemanticRecord semanticRecord : symbolTable.semanticRecordEntries.values()) {
				//assign unique memory addresses to variables, parameter and function kinds
				if(!semanticRecord.idKind.equals("class")) {
					semanticRecord.memoryAddress = semanticRecord.idName + uniqueNumber;
					uniqueNumber++;
				}
			}
		}
		tempAddress = "temp" + uniqueNumber;
		uniqueNumber++;
	}
	
	//TODO: incomplete does not compute class memory size or class variable declarations 
	public static void computeMemorySizes() {
		//iterate through symbol tables update int and float types
		for(SymbolTable symbolTable : SemanticAnalyzer.symbolTableList) {
			for(SemanticRecord semanticRecord : symbolTable.semanticRecordEntries.values()) {
				if(semanticRecord.idKind.equals("variable") || semanticRecord.idKind.equals("parameter") 
						|| (semanticRecord.idKind.equals("function") && !semanticRecord.idName.equals("program"))) {
					if(semanticRecord.idType.equals("int")) {
						semanticRecord.memorySize *= INT_MEMORY_SIZE;
					} else if(semanticRecord.idType.equals("float")) {
						semanticRecord.memorySize *= FLOAT_MEMORY_SIZE;
					}
				}
			}
		}
		
		//compute class memory sizes
		
		//compute memory size for class declarations
	}
	
	public static void computeClassMemorySizes() {
		//iterate through global symbol table to calculate class memory sizes 
	}
	
	public static void updateTempAddress() {
		tempAddress = "temp" + uniqueNumber;
		uniqueNumber++;
	}
	
	//code generation templates
	//r1: operand 1, r2: operand 2, r3: accumulator, r4-r14: parameters, r15: return address
	
	public static void cGen(String string) {
		moonInstructionList.add(string);
	}
	
	public static void cGenResMem(SemanticRecord declaration) {
		moonInstructionList.add(declaration.memoryAddress + " res " + declaration.memorySize);
		if(declaration.dimensions.size() > 0) {
			moonInstructionList.add(declaration.memoryAddress + "offset" + " res " + INT_MEMORY_SIZE);
		}
		
	}
	//TODO: (and or, not) not working (bitwise -> logical), no float support
	public static void cGenExpr(SemanticRecord operand1, SemanticRecord operand2, TokenType operator) {
		String moonOpCode = moonOpMap.get(operator);

		//perform idType check before in parse type match error

		if((operand1.idKind.equals("variable") || operand1.idKind.equals("parameter")) 
			&& (operand2.idKind.equals("variable") || operand2.idKind.equals("parameter"))) {
			moonInstructionList.add("lw r1, " + operand1.memoryAddress + "(r0)");
			moonInstructionList.add("lw r2, " + operand2.memoryAddress + "(r0)");	
			moonInstructionList.add(moonOpCode + " r3, r1, r2");
			moonInstructionList.add(tempAddress + " res " + INT_MEMORY_SIZE);
			moonInstructionList.add("sw " + tempAddress + "(r0), r3");			
		} else if(operand1.idKind.equals("variable") || operand1.idKind.equals("parameter") && operand2.idKind.equals("numliteral")) {			
			moonInstructionList.add("lw r1, " + operand1.memoryAddress + "(r0)");
			moonInstructionList.add(moonOpCode + "i" + " r3, r1, " + operand2.intValue);
			moonInstructionList.add(tempAddress + " res " + INT_MEMORY_SIZE);
			moonInstructionList.add("sw " + tempAddress + "(r0), r3");		
		} else if(operand1.idKind.equals("numliteral") && operand2.idKind.equals("variable") || operand2.idKind.equals("parameter")) {			
			moonInstructionList.add("addi r1, r0, " + operand1.intValue);
			moonInstructionList.add("lw r2, " + operand2.memoryAddress + "(r0)");
			moonInstructionList.add(moonOpCode + " r3, r1, r2");
			moonInstructionList.add(tempAddress + " res " + INT_MEMORY_SIZE);
			moonInstructionList.add("sw " + tempAddress + "(r0), r3");		
		} else if(operand1.idKind.equals("numliteral") && operand2.idKind.equals("numliteral")) {		
			moonInstructionList.add("addi r1, r0, " + operand1.intValue);
			moonInstructionList.add(moonOpCode + "i" + " r3, r1, " + operand2.intValue);
			moonInstructionList.add(tempAddress + " res " + INT_MEMORY_SIZE);
			moonInstructionList.add("sw " + tempAddress + "(r0), r3");
		}		
	}

	public static void cGenAssign(SemanticRecord variable, SemanticRecord expr) {	
		
		if(expr.idKind.equals("variable") || expr.idKind.equals("parameter")) {
			moonInstructionList.add("lw r3, " + expr.memoryAddress + "(r0)");
			moonInstructionList.add("sw " + variable.memoryAddress + "(r0), r3");
		} else if(expr.idKind.equals("numliteral")) {
			moonInstructionList.add("addi r3, r0, " + expr.intValue);
			moonInstructionList.add("sw " + variable.memoryAddress + "(r0), r3");
		} else if(expr.idKind.equals("function")) {
			moonInstructionList.add("lw r3, " + expr.memoryAddress + "res" + "(r0)");
			moonInstructionList.add("sw " + variable.memoryAddress + "(r0), r3");
		}
		
	}
	
	//TODO: only inputs 1 byte
	public static void cGenGet(SemanticRecord variable) {
		moonInstructionList.add("sub r3,r3,r3");
		moonInstructionList.add("getc r3");
		moonInstructionList.add("sw " + variable.memoryAddress + "(r0), r3");
	}
	
	//TODO: only outputs 1 byte
	public static void cGenPut(SemanticRecord expr) {
		
		if (expr.idKind.equals("variable") || expr.idKind.equals("parameter")) {			
			moonInstructionList.add("lw r3, " + expr.memoryAddress + "(r0)");
			moonInstructionList.add("putc r3");
		} else if(expr.idKind.equals("numliteral")) {
			moonInstructionList.add("addi r3, r0, " + expr.intValue);
			moonInstructionList.add("putc r3");
		}
	}
	
	//jump labels specified in second pass parser
	public static void cGenBranch(SemanticRecord expr, String elseLabel) {
		moonInstructionList.add("lw r3, " + expr.memoryAddress + "(r0)");
		moonInstructionList.add("bz r3, " + elseLabel);
	}
	
	//TODO: bug - update expr in for loop occurs after this
	public static void cGenLoop(SemanticRecord relExpr, String endWhileLabel) {
		moonInstructionList.add("lw r3, " + relExpr.memoryAddress + "(r0)");
		moonInstructionList.add("bz r3, " + endWhileLabel);
	}
	
	public static void cGenFunctionRes(SemanticRecord function) {
		moonInstructionList.add(function.memoryAddress + "res" +  " res " + function.memorySize);
		moonInstructionList.add(function.memoryAddress); //function jump label before param res
	}
	
	public static void cGenFunctionStart(SemanticRecord function) {
		int paramReg = 4;
		//moonInstructionList.add(function.memoryAddress); function jump label after param res?
		for(SemanticRecord parameter : function.parameterReferences) {
			moonInstructionList.add("sw " + parameter.memoryAddress + "(r0), r" + paramReg);
			paramReg++;
		}
	}
	
	public static void cGenFunctionEnd(SemanticRecord function, SemanticRecord returnExpr) {
		if(returnExpr.idKind.equals("variable") || returnExpr.idKind.equals("parameter")) {
			moonInstructionList.add("lw r3, " + returnExpr.memoryAddress + "(r0)");		
		} else if(returnExpr.idKind.equals("numliteral") && returnExpr.idType.equals("int")) {
			moonInstructionList.add("addi r3, r0, " + returnExpr.intValue);
		}
		moonInstructionList.add("sw " + function.memoryAddress + "res" + "(r0), r3");
		moonInstructionList.add("j r15");
	}
	
	public static void cGenFunctionCall(SemanticRecord function, ArrayList<SemanticRecord> argumentList) {
		int paramReg = 4;
		for(SemanticRecord argument : argumentList) {
			if(argument.idKind.equals("variable") || argument.idKind.equals("parameter")) {
				moonInstructionList.add("lw "  + "r" + paramReg  +", " + argument.memoryAddress + "(r0)");
				paramReg++;
			} else if(argument.idKind.equals("numliteral") && argument.idType.equals("int")) {
				moonInstructionList.add("addi "  + "r" + paramReg  +", r0, " + argument.intValue);
				paramReg++;
			}
		}
		moonInstructionList.add("lw r3, " + function.memoryAddress + "(r0)");	
		moonInstructionList.add("jlr r15, r3");
	}
	
	//TODO: unused, incomplete
	//outputs code for computing offset using row-major order
	public static void cGenUpdateOffset(SemanticRecord variable, SemanticRecord indiceExpr) {
		int product = 1; //product of N(k+1) -> N(d)
		for(int index = variable.indiceIndex + 1; index < variable.dimensions.size(); index++) {
			product *= variable.dimensions.get(index);
		}
		
		if(indiceExpr.idKind.equals("variable") || indiceExpr.idKind.equals("parameter")) {
			moonInstructionList.add("lw r1, " + variable.memoryAddress + "offset(r0)");
			moonInstructionList.add("lw r2, " + indiceExpr.memoryAddress + "(r0)");
			moonInstructionList.add("muli r2, r2, " + product);
			moonInstructionList.add("add r3, r1, r2");
			moonInstructionList.add("sw " + variable.memoryAddress + "offset(r0), r3");	
		} else if(indiceExpr.idKind.equals("numliteral") && indiceExpr.idType.equals("int")) {
			moonInstructionList.add("lw r1, " + variable.memoryAddress + "offset(r0)");
			moonInstructionList.add("addi r2, r0, " + indiceExpr.intValue);
			moonInstructionList.add("muli r2, r2, " + product);
			moonInstructionList.add("add r3, r1, r2");
			moonInstructionList.add("sw " + variable.memoryAddress + "offset(r0), r3");	
		}
		
		variable.indiceIndex++;
	}
}
