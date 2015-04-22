import java.io.PrintWriter;
import java.util.ArrayList;


public class SemanticAnalyzer {
	public static SymbolTable currentSymbolTable = null;
	public static SymbolTable globalSymbolTable = null;
	public static ArrayList<SymbolTable> symbolTableList = new ArrayList<SymbolTable>(); 
	
	//Semantic error reporting
	public static ArrayList<String> semanticErrorList = new ArrayList<String>();
	public static Boolean semanticParseSuccess = true;
	public static int semanticErrorCount = 0;
	
	public static SymbolTable createSymbolTable(String name) {
		SymbolTable symbolTable = new SymbolTable();
		symbolTable.parentSymbolTable = currentSymbolTable;
		symbolTable.name = name;
		return symbolTable;
	}
		
	public static void enterScope(SymbolTable symbolTable) {
		currentSymbolTable = symbolTable;
	}
	
	public static void exitScope(){
		currentSymbolTable = currentSymbolTable.parentSymbolTable;
	}
	
	//insert semantic records into symbol table
	public static void insert(SemanticRecord semanticRecord) {	
		//check for multiple declaration errors and report; same id names within scope not allowed
		if(currentSymbolTable.semanticRecordEntries.containsValue(semanticRecord)) {
			semanticErrorList.add("Multiple Declaration Error \nIdentifier name: " + semanticRecord.idName + "\nScope: " + currentSymbolTable.name + "\nLine number: " + (FirstPassParser.lookahead.lineNumber - 1));
			semanticParseSuccess = false;
			semanticErrorCount++;
		}
		String key = semanticRecord.idName;
		currentSymbolTable.semanticRecordEntries.put(key, semanticRecord);
	}
	
	//searches symbol table and parent tables for semantic record, if not found records undeclared variable error
	public static SemanticRecord search(String idNameMatch, SymbolTable searchedSymbolTable) {
		while(searchedSymbolTable != null) {
			if(searchedSymbolTable.semanticRecordEntries.containsKey(idNameMatch)) {
				return searchedSymbolTable.semanticRecordEntries.get(idNameMatch);
			}
			searchedSymbolTable = searchedSymbolTable.parentSymbolTable;
		}
		
		semanticErrorList.add("Undefined Identifier Error \nIdentifier name: " + idNameMatch + "\nScope: " + currentSymbolTable.name + "\nLine number: " + SecondPassParser.lookahead.lineNumber);
		semanticParseSuccess = false;
		semanticErrorCount++;
		return new SemanticRecord("undeclared","undeclared","undeclared","undeclared"); 
	}
	
	//check if data type is defined for variable, parameter, function declarations
	public static void checkDataTypeDefined(String dataType) {
		if(globalSymbolTable.semanticRecordEntries.containsKey(dataType)) {
			return;
		}
		
		semanticErrorList.add("Undefined Data Type Error \nIdentifier name: " + dataType + "\nScope: " + currentSymbolTable.name + "\nLine number: " + SecondPassParser.lookahead.lineNumber);
		semanticParseSuccess = false;
		semanticErrorCount++;
		return;
	}
	
	//used for entering/exiting scopes on second pass
	public static SymbolTable getTableReference(String scopeName) {
		return currentSymbolTable.semanticRecordEntries.get(scopeName).symbolTableReference;
	}
	
	public static void checkTypeMatch(SemanticRecord operand1, SemanticRecord operand2) {
		if(operand1.idType.equals(operand2.idType)) {
			return;
		}
	
		semanticErrorList.add("Type Mismatch Error \nFirst operand type: " + operand1.idType + "\nSecond operand type: " + operand2.idType 
								+ "\nScope: " + currentSymbolTable.name + "\nLine number: " + SecondPassParser.lookahead.lineNumber);
		semanticParseSuccess = false;
		semanticErrorCount++;
		return;
	}
	
	public static void checkDimensionMatch(SemanticRecord factor) {
		if(factor.dimensionCounter == factor.dimensions.size()) {
			return;
		}
		semanticErrorList.add("Dimension Mismatch Error \nIdentifier name: " + factor.idName + "\nNumber dimensions expected: " + factor.dimensions.size() 
				 + "\nNumber dimensions provided: " + factor.dimensionCounter + "\nScope: " + currentSymbolTable.name + "\nLine number: " + SecondPassParser.lookahead.lineNumber);
		semanticParseSuccess = false;
		semanticErrorCount++;
		return;
	}
	
	public static void checkArgumentMatch(SemanticRecord function, ArrayList<SemanticRecord> argumentList) {
		if(function.parameterReferences.size() != argumentList.size()) {
			semanticErrorList.add("Argument Number Mismatch Error \nCalling function: " + function.idName + "\nNumber arguments expected : " + function.parameterReferences.size()
									+ "\nNumber arguments provided : " + argumentList.size()+ "\nScope: " + currentSymbolTable.name + "\nLine number: " + SecondPassParser.lookahead.lineNumber);
			semanticParseSuccess = false;
			semanticErrorCount++;
			return;		
		}
		
		for(int index = 0; index < argumentList.size(); index++) {
			if(!function.parameterReferences.get(index).idType.equals(argumentList.get(index).idType)) {
				semanticErrorList.add("Argument Type Mismatch Error \nCalling function: " + function.idName + "\nType expected : " + function.parameterReferences.get(index).idType
			 						+ "\nType provided : " + argumentList.get(index).idType + "\nScope: " + currentSymbolTable.name + "\nLine number: " + SecondPassParser.lookahead.lineNumber);
				semanticParseSuccess = false;
				semanticErrorCount++;
				return;	
			}
		}
	}
	
	public static void addCurrentTableToList() {
		symbolTableList.add(currentSymbolTable);
	}
	
	//used for checking if a type is defined
	public static void setGlobalSymbolTable() {
		globalSymbolTable = currentSymbolTable;
	}
	
	public static SymbolTable getLocalClassTable(String idType) {
		return globalSymbolTable.semanticRecordEntries.get(idType).getSymbolTableReference();
	}
	
}
