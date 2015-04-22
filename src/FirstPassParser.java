import java.util.ArrayList;


public class FirstPassParser {

	public static Token lookahead = new Token();
	public static ArrayList<String> derivationSequence = new ArrayList<String>();
	
	//Syntax error reporting
	public static ArrayList<String> syntaxErrorList = new ArrayList<String>();
	public static Boolean syntaxParseSuccess = true;
	public static int syntaxErrorCount = 0;
	
	public static void parse() {
		lookahead = LexicalAnalyzer.getNextToken();
		prog();
		match(TokenType.ENDOFFILE);				
	}
	
	public static void match(TokenType input) { 
		if(input == lookahead.type) {
			derivationSequence.add("Match success " + input);
			lookahead = LexicalAnalyzer.getNextToken();
		} else {
			syntaxErrorList.add("Match Error \nExpected token: " + input + "\nLine number: " + lookahead.lineNumber);
			syntaxParseSuccess = false;
			syntaxErrorCount++;
			lookahead = LexicalAnalyzer.getNextToken();
			return;
		}
	}
	
	//error recovery
	public static void skipProductionError(String production) {
		
		syntaxErrorList.add("Production Error \nProduction: " + production + "\nCurrent Token: " + lookahead.type + "\nLine number: " + lookahead.lineNumber );
		syntaxParseSuccess = false;
		syntaxErrorCount++;
		while(lookahead.type != TokenType.SEMICOLON && lookahead.type != TokenType.ENDOFFILE) {
			lookahead = LexicalAnalyzer.getNextToken();
		}
		return;	
	}
	
	public static void printDerivationSequence() {
		for(String derivation : derivationSequence) {
			System.out.println(derivation);
		}	
	}
	
	//NON TERMINAL FUNCTIONS
	
	//1.	<prog> ::= <classDecl><progBody>
	//1.	FIRST(<prog>) = { CLASS , PROGRAM }
	
	public static void prog() {
	
			switch(lookahead.type) {
				case CLASS :
				case PROGRAM :
					derivationSequence.add("<prog> ::= <classDecl><progBody>");
					
					//SEMANTIC ACTIONS: create global table; enter scope
					SemanticAnalyzer.enterScope(SemanticAnalyzer.createSymbolTable("global"));
					SemanticAnalyzer.addCurrentTableToList();
					SemanticAnalyzer.setGlobalSymbolTable();
					
					classDecl();
					progBody();
					break;
				default :
					skipProductionError("prog");			
			}
	}
	
	//2.	<classDecl> ::= class id {<varDeclFuncDefHead>}; <classDecl> | e
	//2.	FIRST(<classDecl>) = { CLASS, EPSILON }
	//1.	FOLLOW(<classDecl>) = { PROGRAM }
	
	public static void classDecl()  {
		
		//SEMANTIC ACTIIONS: gather class semantic info
		SemanticRecord classSemRec = new SemanticRecord("class");
	
		switch(lookahead.type) {
			case CLASS :
				derivationSequence.add("<classDecl> ::= class id {<varDeclFuncDefHead>}; <classDecl>");
				match(TokenType.CLASS);
				
				//SEMANTIC ACTIONS: create class table; enter scope
				SemanticAnalyzer.enterScope(SemanticAnalyzer.createSymbolTable(lookahead.lexeme));
				SemanticAnalyzer.addCurrentTableToList();
				classSemRec.setIdName(lookahead.lexeme);
				classSemRec.setSymbolTableReference(SemanticAnalyzer.currentSymbolTable);
				
				match(TokenType.IDENTIFIER);
				match(TokenType.OPENCURLYPAR);
				varDeclFuncDefHead();
				match(TokenType.CLOSECURLYPAR);
				match(TokenType.SEMICOLON);
				
				//SEMANTIC ACTIONS: exit scope; insert class entry
				SemanticAnalyzer.exitScope();
				SemanticAnalyzer.insert(classSemRec);
				
				classDecl();
				break;
			case PROGRAM :
				derivationSequence.add("<classDecl> ::= EPSILON");
				break;
			default :
				skipProductionError("classDecl");			
		}		
	}
	
	//3.	<varDeclFuncDefHead> ::= <type> id <varDeclFuncDefTail> | e
	//3.	FIRST(<varDeclFuncDefHead>) = { FLOAT, IDENTIFIER, INT, EPSILON } 
	//2.	FOLLOW(<varDeclFuncDefHead>) = { CLOSECURLYPAR }
	
	public static void varDeclFuncDefHead() {
	
		//SEMANTIC ACTIONS: gather variable or function semantic info (varOrFuncSemRec)
		SemanticRecord varOrFuncSemRec = new SemanticRecord();
		
		switch(lookahead.type) {
			case INT :
			case FLOAT :
			case IDENTIFIER :
				derivationSequence.add("<varDeclFuncDefHead> ::= <type> id <varDeclFuncDefTail>");
				type(varOrFuncSemRec);
				
				//SEMANTIC ACTIONS: set semantic record name
				varOrFuncSemRec.setIdName(lookahead.lexeme);
				
				match(TokenType.IDENTIFIER);
				varDeclFuncDefTail(varOrFuncSemRec);
				break;
			case CLOSECURLYPAR :
				derivationSequence.add("<varDeclFuncDefHead> ::= EPSILON");
				break;
			default :
				skipProductionError("varDeclFuncDefHead");			
		}		
	}	
	
	//4.	<varDeclFuncDefTail> ::= <arraySize>;< varDeclFuncDefHead > | (<fParams>)<funcBody>;<funcDef>
	//4.	FIRST(<varDeclFuncDefTail>) = { OPENSQUAREPAR, SEMICOLON, OPENPAR } 
	 
	public static void varDeclFuncDefTail(SemanticRecord varOrFuncSemRec) {

		switch(lookahead.type) {
			case OPENSQUAREPAR :
			case SEMICOLON :
				derivationSequence.add("<varDeclFuncDefTail> ::= <arraySize>;< varDeclFuncDefHead >");
				
				//SEMANTIC ACTIONS
				varOrFuncSemRec.setIdKind("variable");
				
				arraySize(varOrFuncSemRec);
				match(TokenType.SEMICOLON);
				
				//SEMANTIC ACTIONS: insert variable entry (varOrFuncSemRec)
				SemanticAnalyzer.insert(varOrFuncSemRec);
				
				varDeclFuncDefHead();
				break;
			case OPENPAR :
				derivationSequence.add("<varDeclFuncDefTail> ::= (<fparams>)<funcBody>;<funcDef>");
				
				//SEMANTIC ACTIONS: create function table (use semantic record for name); enter scope
				SemanticAnalyzer.enterScope(SemanticAnalyzer.createSymbolTable(varOrFuncSemRec.getIdName()));
				SemanticAnalyzer.addCurrentTableToList();
				varOrFuncSemRec.setIdKind("function");
				varOrFuncSemRec.setSymbolTableReference(SemanticAnalyzer.currentSymbolTable);
				
				match(TokenType.OPENPAR);
				fParams(varOrFuncSemRec);
				match(TokenType.CLOSEPAR);
				funcBody();
				match(TokenType.SEMICOLON);
				
				//SEMANTIC ACTIONS: exit scope; insert function entry (varOrFuncSemRec)
				SemanticAnalyzer.exitScope();
				SemanticAnalyzer.insert(varOrFuncSemRec);
				
				funcDef();
				break;
			default :
				skipProductionError("varDeclFuncDefTail");			
		}		
	}
	
	//5.	<progBody> ::= program<funcBody>;<funcDef>
	//5.	FIRST(<progBody>) = { PROGRAM }
	
	public static void progBody() {
		
		//SEMANTIC ACTIONS: gather program semantic info
		SemanticRecord funcSemRec = new SemanticRecord("function");
		funcSemRec.setIdName("program");
		
		switch(lookahead.type) {
			case PROGRAM :
				derivationSequence.add("<progBody> ::= program<funcBody>;<funcDef>");
				match(TokenType.PROGRAM);
				
				
				//SEMANTIC ACTIONS: create program table; enter scope
				SemanticAnalyzer.enterScope(SemanticAnalyzer.createSymbolTable("program"));
				SemanticAnalyzer.addCurrentTableToList();
				funcSemRec.setSymbolTableReference(SemanticAnalyzer.currentSymbolTable);
				
				funcBody();
				match(TokenType.SEMICOLON);
				
				//SEMANTIC ACTIONS: exit scope; insert program(function) entry
				SemanticAnalyzer.exitScope();
				SemanticAnalyzer.insert(funcSemRec);
				
				funcDef();
				break;
			default :
				skipProductionError("progBody");				
		}		
			
	}
	
	//6.	<funcHead> ::= <type>id(<fParams>)
	//6.	FIRST(<funcHead>) = { INT , FLOAT , IDENTIFIER }
	
	public static void funcHead(SemanticRecord funcSemRec) {
		
		switch(lookahead.type) {
			case INT :
			case FLOAT :
			case IDENTIFIER :
				derivationSequence.add("<funcHead> ::= <type>id(<fParams>)");	
				type(funcSemRec);
				
				//SEMANTIC ACTIONS:
				SemanticAnalyzer.currentSymbolTable.setName(lookahead.lexeme);
				funcSemRec.setIdName(lookahead.lexeme);
				funcSemRec.setSymbolTableReference(SemanticAnalyzer.currentSymbolTable);
				
				match(TokenType.IDENTIFIER);
				match(TokenType.OPENPAR);
				fParams(funcSemRec);
				match(TokenType.CLOSEPAR);
				break;
			default :
				skipProductionError("funcHead");			
		}		
			
	}
	
	//7.	<funcDef> ::= <funcHead><funcBody>; <funcDef> | e
	//7.	FIRST(<funcDef>) = { INT , FLOAT , IDENTIFIER , EPSILON }
	//3.	FOLLOW(<funcDef>) = { CLOSECURLYPAR , ENDOFFILE }
	
	public static void funcDef() {
		
		//SEMANTIC ACTIONS: gather function semantic info
		SemanticRecord funcSemRec = new SemanticRecord("function");
		
		switch(lookahead.type) {
			case INT :
			case FLOAT :
			case IDENTIFIER :
				derivationSequence.add("<funcDef> ::= <funcHead><funcBody>; <funcDef>");
				
				//SEMANTIC ACTIONS: create function table; enter scope
				SemanticAnalyzer.enterScope(SemanticAnalyzer.createSymbolTable(null));
				SemanticAnalyzer.addCurrentTableToList();
				
				funcHead(funcSemRec);
				funcBody();
				match(TokenType.SEMICOLON);
				
				//SEMANTIC ACTIONS: exit scope; insert function entry
				SemanticAnalyzer.exitScope();
				SemanticAnalyzer.insert(funcSemRec);
				
				funcDef();		
				break;
			case CLOSECURLYPAR :
			case ENDOFFILE :
				derivationSequence.add("<funcDef> ::= EPSILON");
				break;
			default :
				skipProductionError("funcDef");			
		}			
	}
	
	//8.	<funcBody> ::= {<varDeclStatementHead>}
	//8.	FIRST(<funcBody>) = { OPENCURLYPAR } 
	
	public static void funcBody() {
		
		switch(lookahead.type) {
			case OPENCURLYPAR :
				derivationSequence.add("<funcBody> ::= {<varDeclStatementHead>}");
				match(TokenType.OPENCURLYPAR);
				varDeclStatementHead();
				match(TokenType.CLOSECURLYPAR);
				break;
			default :
				skipProductionError("funcBody");			
		}	
	}
	
	/*9.	<varDeclStatementHead> ::= int id<arraySize>; <varDeclStatementHead> 
	| float id<arraySize>; <varDeclStatementHead>  
	| if(<expr>)then<statBlock>else<statBlock>; <statement>
	| for(<type>id<assignOp><expr>;<relExpr>;<assignStat>)<statBlock>; <statement>
	| get(<variable>); <statement>
	| put(<expr>); <statement>
	| return(<expr>); <statement>
	| id <varDeclStatementTail>
	| e
	*/
	//9.	FIRST(<varDeclStatementHead>) = { IDENTIFIER, INT, FLOAT, RETURN, PUT, GET, IF, FOR, EPSILON } 
	//4.	FOLLOW(<varDeclStatementHead>) = { CLOSECURLYPAR } 

	public static void varDeclStatementHead() {
		
		//SEMANTIC ACTIONS: gather variable semantic info
		//SEMANTIC ACTIONS: declaredVariableMatch used to search if it is an assignment 
		SemanticRecord varSemRec = new SemanticRecord("variable");
		
		switch(lookahead.type) {
			case INT :
				derivationSequence.add("<varDeclStatementHead> ::= int id<arraySize>; <varDeclStatementHead>");
				varSemRec.setIdType("int");
				
				match(TokenType.INT);
				
				//SEMANTIC ACTIONS: name
				varSemRec.setIdName(lookahead.lexeme);
				
				match(TokenType.IDENTIFIER);
				arraySize(varSemRec);
				match(TokenType.SEMICOLON);
				
				//SEMANTIC ACTIONS: insert variable entry
				SemanticAnalyzer.insert(varSemRec);
				
				varDeclStatementHead();
				break;
			case FLOAT :
				derivationSequence.add("<varDeclStatementHead> ::= float id<arraySize>; <varDeclStatementHead>");
				varSemRec.setIdType("float");
			
				match(TokenType.FLOAT);
				
				//SEMANTIC ACTIONS: name
				varSemRec.setIdName(lookahead.lexeme);
				
				match(TokenType.IDENTIFIER);
				arraySize(varSemRec);
				match(TokenType.SEMICOLON);
				
				//SEMANTIC ACTIONS: insert variable entry
				SemanticAnalyzer.insert(varSemRec);
				
				varDeclStatementHead();			
				break;
			case IF :
				derivationSequence.add("<varDeclStatementHead> ::= if(<expr>)then<statBlock>else<statBlock>; <statement>");
				match(TokenType.IF);
				match(TokenType.OPENPAR);
				expr();
				match(TokenType.CLOSEPAR);
				match(TokenType.THEN);
				statBlock();
				match(TokenType.ELSE);
				statBlock();
				match(TokenType.SEMICOLON);
				statement();
				break;
			case FOR :
				derivationSequence.add("<varDeclStatementHead> ::= for(<type>id<assignOp><expr>;<relExpr>;<assignStat>)<statBlock>; <statement>");
				match(TokenType.FOR);
				match(TokenType.OPENPAR);
				type(varSemRec);
				
				//SEMANTIC ACTIONS: name
				varSemRec.setIdName(lookahead.lexeme);
				
				match(TokenType.IDENTIFIER);
				assignOp();
				expr();
				match(TokenType.SEMICOLON);
				
				//SEMANTIC ACTIONS: insert variable entry
				SemanticAnalyzer.insert(varSemRec);
				
				relExpr();
				match(TokenType.SEMICOLON);
				assignStat();
				match(TokenType.CLOSEPAR);
				statBlock();
				match(TokenType.SEMICOLON);
				statement();
				break;
			case GET :
				derivationSequence.add("<varDeclStatementHead> ::= get(<variable>); <statement>");
				match(TokenType.GET);
				match(TokenType.OPENPAR);
				variable();
				match(TokenType.CLOSEPAR);
				match(TokenType.SEMICOLON);
				statement();
				break;
			case PUT :
				derivationSequence.add("<varDeclStatementHead> ::= put(<expr>); <statement>");
				match(TokenType.PUT);
				match(TokenType.OPENPAR);
				expr();
				match(TokenType.CLOSEPAR);
				match(TokenType.SEMICOLON);
				statement();				
				break;
			case RETURN :
				derivationSequence.add("<varDeclStatementHead> ::= return(<expr>); <statement>");
				match(TokenType.RETURN);
				match(TokenType.OPENPAR);
				expr();
				match(TokenType.CLOSEPAR);
				match(TokenType.SEMICOLON);
				statement();
				break;
			case IDENTIFIER :
				derivationSequence.add("<varDeclStatementHead> ::= id <varDeclStatementTail>");
				
				varSemRec.setIdType(lookahead.lexeme);
				
				match(TokenType.IDENTIFIER);
				varDeclStatementTail(varSemRec);
				break;
			case CLOSECURLYPAR :
				derivationSequence.add("<varDeclStatementHead> ::= EPSILON");
				break;
			default :
				skipProductionError("varDeclStatementHead");			
		}	
	}	
	
	/*
	10.	<varDeclStatementTail> ::= id <arraySize>; <varDeclStatementHead> 
	| <indice><idnest><assignOp><expr> ; <statement>
	*/
	//10.	FIRST(<varDeclStatementTail>) = { IDENTIFIER, OPENSQUAREPAR, PERIOD, ASSIGNMENT } 
	
	public static void varDeclStatementTail(SemanticRecord varSemRec) {
		
		switch(lookahead.type) {
			case IDENTIFIER :
				derivationSequence.add("<varDeclStatementTail> ::= id <arraySize>; <varDeclStatementHead>");
				
				//SEMANTIC ACTIONS: name
				varSemRec.setIdName(lookahead.lexeme);
				
				match(TokenType.IDENTIFIER);
				arraySize(varSemRec);
				match(TokenType.SEMICOLON);
				
				//SEMANTIC ACTIONS: insert variable entry
				SemanticAnalyzer.insert(varSemRec);
				
				varDeclStatementHead();
				break;
			case OPENSQUAREPAR :
			case PERIOD :
			case ASSIGNMENT :
				derivationSequence.add("<varDeclStatementTail> ::= <indice><idnest><assignOp><expr> ; <statement>");
				indice();
				idnest();
				assignOp();
				expr();
				match(TokenType.SEMICOLON);
				statement();
				break;
			default :
				skipProductionError("varDeclStatementTail");			
		}	
	}
	
	/*
	11.	<statement> ::= <assignStat>; <statement>
	| if(<expr>)then<statBlock>else<statBlock>; <statement>
	| for(<type>id<assignOp><expr>;<relExpr>;<assignStat>)<statBlock>; <statement>
	| get(<variable>); <statement>
	| put(<expr>); <statement>
	| return(<expr>); <statement>
	| e
	*/
	//11.	FIRST(<statement>) = { IDENTIFIER , IF , FOR , GET , PUT , RETURN , EPSILON }
	//5.	FOLLOW(<statement>) = { CLOSECURLYPAR } 
	
	public static void statement() {
		
		//SEMANTIC ACTIONS: gather variable semantic info
		SemanticRecord varSemRec = new SemanticRecord("variable");
		
		switch(lookahead.type) {
			case IDENTIFIER :
				derivationSequence.add("<statement> ::= <assignStat>; <statement>");
				assignStat();
				match(TokenType.SEMICOLON);
				statement();
				break;
			case IF :
				derivationSequence.add("<statement> ::= if(<expr>)then<statBlock>else<statBlock>; <statement>");
				match(TokenType.IF);
				match(TokenType.OPENPAR);
				expr();
				match(TokenType.CLOSEPAR);
				match(TokenType.THEN);
				statBlock();
				match(TokenType.ELSE);
				statBlock();
				match(TokenType.SEMICOLON);
				statement();
				break;
			case FOR :
				derivationSequence.add("<statement> ::= for(<type>id<assignOp><expr>;<relExpr>;<assignStat>)<statBlock>; <statement>");
				match(TokenType.FOR);
				match(TokenType.OPENPAR);
				type(varSemRec);
				
				//SEMANTIC ACTIONS: name
				varSemRec.setIdName(lookahead.lexeme);
				
				match(TokenType.IDENTIFIER);
				assignOp();
				expr();
				match(TokenType.SEMICOLON);
				
				//SEMANTIC ACTIONS: insert variable entry
				SemanticAnalyzer.insert(varSemRec);
				
				relExpr();
				match(TokenType.SEMICOLON);
				assignStat();
				match(TokenType.CLOSEPAR);
				statBlock();
				match(TokenType.SEMICOLON);
				statement();
				break;
			case GET :
				derivationSequence.add("<statement> ::= get(<variable>); <statement>");
				match(TokenType.GET);
				match(TokenType.OPENPAR);
				variable();
				match(TokenType.CLOSEPAR);
				match(TokenType.SEMICOLON);
				statement();
				break;
			case PUT :
				derivationSequence.add("<statement> ::= put(<expr>); <statement>");
				match(TokenType.PUT);
				match(TokenType.OPENPAR);
				expr();
				match(TokenType.CLOSEPAR);
				match(TokenType.SEMICOLON);
				statement();				
				break;
			case RETURN :
				derivationSequence.add("<statement> ::= return(<expr>); <statement>");
				match(TokenType.RETURN);
				match(TokenType.OPENPAR);
				expr();
				match(TokenType.CLOSEPAR);
				match(TokenType.SEMICOLON);
				statement();
				break;
			case CLOSECURLYPAR :
				derivationSequence.add("<statement> ::= EPSILON");
				break;
			default :
				skipProductionError("statement");			
		}	
	}	
	
	//12.	<assignStat> ::= <variable><assignOp><expr>
	//12.	FIRST(<assignStat>) = { IDENTIFIER }
	
	public static void assignStat() {
		
		switch(lookahead.type) {
			case IDENTIFIER :
				derivationSequence.add("<assignStat> ::= <variable><assignOp><expr>");
				variable();
				assignOp();
				expr();	
				break;
			default :
				skipProductionError("assignStat");			
		}		
	}	
	
	//13.	<statBlock> ::= {<statement>} | e
	//13.	FIRST(<statBlock>) = { OPENCURLYPAR, EPSILON }
	//6.	FOLLOW(<statBlock>) = { SEMICOLON, ELSE }

	public static void statBlock() {
		
		switch(lookahead.type) {
			case OPENCURLYPAR :
				derivationSequence.add("<statBlock> ::= {<statement>}");
				match(TokenType.OPENCURLYPAR);
				statement();
				match(TokenType.CLOSECURLYPAR);
				break;
			case SEMICOLON :
			case ELSE :
				derivationSequence.add("<statBlock> ::= EPSILON");
				break;
			default :
				skipProductionError("statBlock");			
		}	
	}
	
	//14.	<expr> ::= <arithExpr><exprTail>
	//14.	FIRST(<expr>) = { IDENTIFIER , NUMINT , NUMFLOAT , OPENPAR , NOT , ADD , SUBTRACT }
	
	public static void expr() {
		
		switch(lookahead.type) {
			case IDENTIFIER :
			case NUMINT :
			case NUMFLOAT :
			case OPENPAR :
			case NOT :
			case ADD :
			case SUBTRACT :
				derivationSequence.add("<expr> ::= <arithExpr><exprTail>");
				arithExpr();
				exprTail();
				break;
			default :
				skipProductionError("expr");			
		}		
	}	
	
	//15.	<exprTail> ::= <relOp><arithExpr> | e 
	//15.	FIRST(<exprTail>) = { LESSTHAN, LESSTHANOREQ, NOTEQUAL, EQUAL, GREATTHAN, GREATTHANOREQ, EPSILON } 
	//7.	FOLLOW(<exprTail>) = { SEMICOLON, CLOSEPAR, COMMA }
	
	public static void exprTail() {
		
		switch(lookahead.type) {
			case LESSTHAN :
			case LESSTHANOREQ :
			case NOTEQUAL :
			case EQUAL :
			case GREATTHAN :
			case GREATTHANOREQ :
				derivationSequence.add("<exprTail> ::= <relOp><arithExpr>");
				relOp();
				arithExpr();
				break;
			case SEMICOLON :
			case CLOSEPAR :
			case COMMA :
				derivationSequence.add("<exprTail> ::= EPSILON");
				break;
			default :
				skipProductionError("exprTail");			
		}		
	}
	
	//16.	<relExpr> ::= <arithExpr><relOp><arithExpr>
	//16.	FIRST(<relExpr>) = { IDENTIFIER , NUMINT , NUMFLOAT , OPENPAR , NOT , ADD , SUBTRACT } 
	
	public static void relExpr() {
		
		switch(lookahead.type) {
			case IDENTIFIER :
			case NUMINT :
			case NUMFLOAT :
			case OPENPAR :
			case NOT :
			case ADD :
			case SUBTRACT :
				derivationSequence.add("<relExpr> ::= <arithExpr><relOp><arithExpr>");
				arithExpr();
				relOp();
				arithExpr();
				break;
			default :
				skipProductionError("relExpr");			
		}		
	}
	
	//17.	<arithExpr> ::= <term><arithExprTail>
	//17.	FIRST(<arithExpr>) = { IDENTIFIER , NUMINT , NUMFLOAT , OPENPAR , NOT , ADD , SUBTRACT } 
	
	public static void arithExpr() {
		
		switch(lookahead.type) {
			case IDENTIFIER :
			case NUMINT :
			case NUMFLOAT :
			case OPENPAR :
			case NOT :
			case ADD :
			case SUBTRACT :
				derivationSequence.add("<arithExpr> ::= <term><arithExprTail>");
				term();
				arithExprTail();
				break;
			default :
				skipProductionError("arithExpr");			
		}		
	}
	
	//18.	<arithExprTail> ::= <addOp><term><arithExprTail> | e
	//18.	FIRST<arithExprTail>) = { ADD, SUBTRACT, OR, EPSILON } 
	//8.	FOLLOW(<arithExprTail>) = { SEMICOLON, CLOSEPAR, COMMA, LESSTHAN, LESSTHANOREQ, NOTEQUAL, EQUAL, GREATTHAN, GREATTHANOREQ, CLOSESQUAREPAR } 
	
	public static void arithExprTail() {
		
		switch(lookahead.type) {
			case ADD :
			case SUBTRACT :
			case OR :
				derivationSequence.add("<arithExprTail> ::= <addOp><term><arithExprTail>");
				addOp();
				term();
				arithExprTail();
				break;
			case SEMICOLON :
			case CLOSEPAR :
			case COMMA :
			case LESSTHAN :
			case LESSTHANOREQ :
			case NOTEQUAL :
			case EQUAL :
			case GREATTHAN :
			case GREATTHANOREQ :
			case CLOSESQUAREPAR :
				derivationSequence.add("<arithExprTail> ::= EPSILON");
				break;
			default :
				skipProductionError("arithExprTail");			
		}		
	}	
	
	//19.	<sign> ::= + | -
	//19.	FIRST(<sign>) = { ADD , SUBTRACT }
	public static void sign() {
		
		switch(lookahead.type) {
			case ADD :
				derivationSequence.add("<sign> ::= +");
				match(TokenType.ADD);
				break;
			case SUBTRACT :
				derivationSequence.add("<sign> ::= -");
				match(TokenType.SUBTRACT);
				break;
			default :
				skipProductionError("sign");			
		}		
	}	
	
	//20.	<term> ::= <factor><termTail>
	//20.	FIRST(<term>) = { IDENTIFIER , NUMINT , NUMFLOAT , OPENPAR , NOT , ADD , SUBTRACT }
	
	public static void term() {
		
		switch(lookahead.type) {
			case IDENTIFIER :
			case NUMINT :
			case NUMFLOAT :
			case OPENPAR :
			case NOT :
			case ADD :
			case SUBTRACT :
				derivationSequence.add("<term> ::= <factor><termTail>");
				factor();
				termTail();
				break;
			default :
				skipProductionError("term");			
		}		
	}
	
	//21.	<termTail> ::= <multOp><factor><termTail> | e
	//21.	FIRST(<termTail>) = { MULTIPLY, DIVIDE, AND, EPSILON }
	//9.	FOLLOW(<termTail>) = { SEMICOLON, CLOSEPAR, COMMA, LESSTHAN, LESSTHANOREQ, NOTEQUAL, EQUAL, GREATTHAN, GREATTHANOREQ, CLOSESQUAREPAR, ADD, SUBTRACT, OR }
	
	public static void termTail() {
		
		switch(lookahead.type) {
			case MULTIPLY :
			case DIVIDE :
			case AND :
				derivationSequence.add("<termTail> ::= <multOp><factor><termTail>");
				multOp();
				factor();
				termTail();
				break;
			case SEMICOLON :
			case CLOSEPAR :
			case COMMA :
			case LESSTHAN :
			case LESSTHANOREQ :
			case NOTEQUAL :
			case EQUAL :
			case GREATTHAN :
			case GREATTHANOREQ :
			case CLOSESQUAREPAR :
			case ADD :
			case SUBTRACT :
			case OR :
				derivationSequence.add("<termTail> ::= EPSILON");
				break;
			default :
				skipProductionError("termTail");			
		}		
	}	
	
	//22.	<factor> ::= id<factorTail>| numint | numfloat | (<arithExpr>) | not<factor> | <sign><factor>
	//22.	FIRST(<factor>) = { IDENTIFIER , NUMINT , NUMFLOAT , OPENPAR , NOT , ADD , SUBTRACT } 
	
	public static void factor() {
		
		switch(lookahead.type) {
			case IDENTIFIER :
				derivationSequence.add("<factor> ::= id<factorTail>");
				match(TokenType.IDENTIFIER);
				factorTail();
				break;
			case NUMINT :
				derivationSequence.add("<factor> ::= numint");
				match(TokenType.NUMINT);
				break;
			case NUMFLOAT :
				derivationSequence.add("<factor> ::= numfloat");
				match(TokenType.NUMFLOAT);
				break;
			case OPENPAR :
				derivationSequence.add("<factor> ::= (<arithExpr>)");
				match(TokenType.OPENPAR);
				arithExpr();
				match(TokenType.CLOSEPAR);
				break;
			case NOT :
				derivationSequence.add("<factor> ::= not<factor>");
				match(TokenType.NOT);
				factor();
				break;
			case ADD :
			case SUBTRACT :
				derivationSequence.add("<factor> ::= <sign><factor>");
				sign();
				factor();
				break;
			default :
				skipProductionError("factor");			
		}	
	}
	
	//23.	<factorTail> ::= <indice><factornest>|(<aParams>)
	//23.	FIRST(<factorTail>) = { OPENSQUAREPAR, PERIOD, OPENPAR, EPSILON }	
	//10.	FOLLOW(<factorTail>) = { SEMICOLON, CLOSEPAR, COMMA, LESSTHAN, LESSTHANOREQ, NOTEQUAL, EQUAL, GREATTHAN, GREATTHANOREQ, CLOSESQUAREPAR, ADD, SUBTRACT, OR, MULTIPLY, DIVIDE, AND }
	
	public static void factorTail() {
		
		switch(lookahead.type) {
			case OPENSQUAREPAR :
			case PERIOD :
				derivationSequence.add("<factorTail> ::= <indice><factornest>");
				indice();
				factornest();
				break;
			case OPENPAR :
				derivationSequence.add("<factorTail> ::= (<aParams>)");
				match(TokenType.OPENPAR);
				aParams();
				match(TokenType.CLOSEPAR);
				break;
			case SEMICOLON :
			case CLOSEPAR :
			case COMMA :
			case LESSTHAN :
			case LESSTHANOREQ :
			case NOTEQUAL :
			case EQUAL :
			case GREATTHAN :
			case GREATTHANOREQ :
			case CLOSESQUAREPAR :
			case ADD :
			case SUBTRACT :
			case OR :
			case MULTIPLY :
			case DIVIDE :
			case AND :
				derivationSequence.add("<factorTail> ::= EPSILON");
				break;
			default :
				skipProductionError("factorTail");			
		}		
	}
	
	//24.	<factornest> ::= . id <factorTail> | e
	//24.	FIRST(<factornest>) = { PERIOD, EPSILON }
	//11.	FOLLOW(<factornest>) = { SEMICOLON, CLOSEPAR, COMMA, LESSTHAN, LESSTHANOREQ, NOTEQUAL, EQUAL, GREATTHAN, GREATTHANOREQ, CLOSESQUAREPAR, ADD, SUBTRACT, OR, MULTIPLY, DIVIDE, AND }
	
	public static void factornest() {
		
		switch(lookahead.type) {
			case PERIOD :
				derivationSequence.add("<factornest> ::= . id <factorTail>");
				match(TokenType.PERIOD);
				match(TokenType.IDENTIFIER);
				factorTail();
				break;
			case SEMICOLON :
			case CLOSEPAR :
			case COMMA :
			case LESSTHAN :
			case LESSTHANOREQ :
			case NOTEQUAL :
			case EQUAL :
			case GREATTHAN :
			case GREATTHANOREQ :
			case CLOSESQUAREPAR :
			case ADD :
			case SUBTRACT :
			case OR :
			case MULTIPLY :
			case DIVIDE :
			case AND :
				derivationSequence.add("<factornest> ::= EPSILON");
				break;
			default :
				skipProductionError("factornest");			
		}		
	}
	
	//25.	<variable> ::= id<indice><idnest>
	//25.	FIRST(<variable>) = { IDENTIFIER }
	
	public static void variable() {
		
		switch(lookahead.type) {
			case IDENTIFIER :
				derivationSequence.add("<variable> ::= id<indice><idnest>");				
				match(TokenType.IDENTIFIER);
				indice();
				idnest();
				break;
			default :
				skipProductionError("variable");			
		}		
	}
	
	//26.	<idnest> ::= . id<indice><idnest> | e
	//26.	FIRST(<idnest>) = { PERIOD , EPSILON }
	//12.	FOLLOW(<idnest>) = { ASSIGNMENT, CLOSEPAR } 
	
	public static void idnest() {
		
		switch(lookahead.type) {
			case PERIOD :
				derivationSequence.add("<idnest> ::= . id<indice><idnest>");
				match(TokenType.PERIOD);
				match(TokenType.IDENTIFIER);
				indice();
				idnest();
				break;
			case ASSIGNMENT :
			case CLOSEPAR :
				derivationSequence.add("<idnest> ::= EPSILON");
				break;
			default :
				skipProductionError("idnest");			
		}		
	}
	
	//27.	<indice> ::= [<arithExpr>] <indice> | e
	//27.	FIRST(<indice>) = { OPENSQUAREPAR , EPSILON }
	//13.	FOLLOW(<indice>) = { PERIOD, ASSIGNMENT, SEMICOLON, CLOSEPAR, COMMA, LESSTHAN, LESSTHANOREQ, NOTEQUAL, EQUAL, GREATTHAN, GREATTHANOREQ, CLOSESQUAREPAR, ADD, SUBTRACT, OR, MULTIPLY, DIVIDE, AND} 

	public static void indice() {
		
		switch(lookahead.type) {
			case OPENSQUAREPAR :
				derivationSequence.add("<indice> ::= [<arithExpr>] <indice>");
				match(TokenType.OPENSQUAREPAR);
				arithExpr();
				match(TokenType.CLOSESQUAREPAR);
				indice();
				break;
			case PERIOD :
			case ASSIGNMENT :
			case SEMICOLON :
			case CLOSEPAR :
			case COMMA :
			case LESSTHAN :
			case LESSTHANOREQ :
			case NOTEQUAL :
			case EQUAL :
			case GREATTHAN :
			case GREATTHANOREQ :
			case CLOSESQUAREPAR :
			case ADD :
			case SUBTRACT :
			case OR :
			case MULTIPLY :
			case DIVIDE :
			case AND :
				derivationSequence.add("<indice> ::= EPSILON");
				break;
			default :
				skipProductionError("indice");			
		}	
	}
	
	//28.	<arraySize> ::= [ numint ] <arraySize> | e
	//28.	FIRST(<arraySize>) = { OPENSQUAREPAR , EPSILON }
	//14.	FOLLOW(<arraySize>) = { SEMICOLON, CLOSEPAR, COMMA } 

	public static void arraySize(SemanticRecord varOrParSemRec) {
		
		switch(lookahead.type) {
			case OPENSQUAREPAR:
				derivationSequence.add("<arraySize> ::= [ numint ] <arraySize>");
				match(TokenType.OPENSQUAREPAR);
				
				//SEMANTIC ACTIONS
				varOrParSemRec.scaleMemorySize(Integer.parseInt(lookahead.lexeme));
				varOrParSemRec.dimensions.add(Integer.parseInt(lookahead.lexeme));
				
				match(TokenType.NUMINT);
				match(TokenType.CLOSESQUAREPAR);
								
				arraySize(varOrParSemRec);			
				break;
			case SEMICOLON :
			case CLOSEPAR :
			case COMMA :
				derivationSequence.add("<arraySize> ::= EPSILON");
				break;
			default :
				skipProductionError("arraySize");			
		}		
	}
	
	//29.	<type> ::= int | float | id
	//29.	FIRST(<type>) = { INT , FLOAT , IDENTIFIER }
	
	public static void type(SemanticRecord varOrParOrFuncSemRec) {
		
		switch(lookahead.type) {
			case INT :
				derivationSequence.add("<type> ::= int");
				
				//SEMANTIC ACTIONS
				varOrParOrFuncSemRec.setIdType("int");
				
				match(TokenType.INT);
				break;
			case FLOAT :
				derivationSequence.add("<type> ::= float");
				
				//SEMANTIC ACTIONS
				varOrParOrFuncSemRec.setIdType("float");
				
				match(TokenType.FLOAT);
				break;
			case IDENTIFIER :
				derivationSequence.add("<type> ::= id");
				
				//SEMANTIC ACTIONS
				varOrParOrFuncSemRec.setIdType(lookahead.lexeme);
				
				match(TokenType.IDENTIFIER);
				break;
			default :
				skipProductionError("type");			
		}		
	}	
	
	//30.	<fParams> ::= <type>id<arraySize><fParamsTail> | e
	//30.	FIRST(<fParams>) = { INT , FLOAT , ID , EPSILON}
	//15.	FOLLOW(<fParams>) = { CLOSEPAR }
	
	public static void fParams(SemanticRecord funcSemRec) {
		
		//SEMANTIC ACTIONS: gather parameter semantic info
		SemanticRecord parSemRec = new SemanticRecord("parameter");
		
		switch(lookahead.type) {
			case INT :
			case FLOAT :
			case IDENTIFIER :
				derivationSequence.add("<fParams> ::= <type>id<arraySize><fParamsTail>");
				type(parSemRec);
				
				//SEMANTIC ACTIONS: name
				parSemRec.setIdName(lookahead.lexeme);
				
				match(TokenType.IDENTIFIER);
				arraySize(parSemRec);
				
				//SEMANTIC ACTIONS: insert parameter entry, add parameter reference for function
				SemanticAnalyzer.insert(parSemRec);
				funcSemRec.addParameterReference(parSemRec);
				
				fParamsTail(funcSemRec);
				break;
			case CLOSEPAR :
				derivationSequence.add("<fParams> ::= EPSILON");
				break;
			default :
				skipProductionError("fParams");			
		}		
	}	
	
	//31.	<aParams> ::= <expr><aParamsTail> | e
	//31.	FIRST(<aParams) = { IDENTIFIER , NUMINT , NUMFLOAT , OPENPAR , NOT , ADD , SUBTRACT, EPSILON }
	//16.	FOLLOW(<aParams>) = { CLOSEPAR } 
	
	public static void aParams() {
		
		switch(lookahead.type) {
			case IDENTIFIER :
			case NUMINT :
			case NUMFLOAT :
			case OPENPAR :
			case NOT :
			case ADD :
			case SUBTRACT :
				derivationSequence.add("<aParams> ::= <expr><aParamsTail>");
				expr();
				aParamsTail();
				break;
			case CLOSEPAR :
				derivationSequence.add("<aParams> ::= EPSILON");
				break;
			default :
				skipProductionError("aParams");			
		}		
	}	
	
	//32.	<fParamsTail> ::= ,<type>id<arraySize> <fParamsTail> | e
	//32.	FIRST(<fParamsTail>) = { COMMA , EPSILON } 
	//17.	FOLLOW(<fParamsTail>) = { CLOSEPAR }

	public static void fParamsTail(SemanticRecord funcSemRec) {
		
		//SEMANTIC ACTIONS: gather parameter semantic info
		SemanticRecord parSemRec = new SemanticRecord("parameter");
		
		switch(lookahead.type) {
			case COMMA :
				derivationSequence.add("<fParamsTail> ::= ,<type>id<arraySize> <fParamsTail>");
				match(TokenType.COMMA);
				type(parSemRec);
				
				//SEMANTIC ACTIONS: name
				parSemRec.setIdName(lookahead.lexeme);
				
				match(TokenType.IDENTIFIER);
				arraySize(parSemRec);
				
				//SEMANTIC ACTIONS: insert parameter entry, add parameter reference for function
				SemanticAnalyzer.insert(parSemRec);
				funcSemRec.addParameterReference(parSemRec);
				
				fParamsTail(funcSemRec);
				break;
			case CLOSEPAR :
				derivationSequence.add("<fParamsTail> ::= EPSILON");
				break;
			default :
				skipProductionError("fParamsTail");			
		}		
	}
	
	//33.	<aParamsTail> ::= ,<expr> <aParamsTail> | e
	//33.	FIRST(<aParamsTail>) = { COMMA , EPSILON }
	//18.	FOLLOW( <aParamsTail>) = { CLOSEPAR }  
	
	public static void aParamsTail() {
		
		switch(lookahead.type) {
			case COMMA :
				derivationSequence.add("<aParamsTail> ::= ,<expr> <aParamsTail>");
				match(TokenType.COMMA);
				expr();
				aParamsTail();
				break;
			case CLOSEPAR :
				derivationSequence.add("<aParamsTail> ::= EPSILON");
				break;
			default :
				skipProductionError("aParamsTail");			
		}		
	}

	//34.	<assignOp> ::= =
	//34.	FIRST(<assignOp>) = { ASSIGNMENT }
	
	public static void assignOp() {
		
		switch(lookahead.type) {
			case ASSIGNMENT :
				derivationSequence.add("<assignOp> ::= =");
				match(TokenType.ASSIGNMENT);
				break;
			default :
				skipProductionError("assignOp");			
		}		
	}	
	
	//35.	<relOp> ::= == | <> | < | > | <= | >=
	//35.	FIRST(<relOp>) = { LESSTHAN, LESSTHANOREQ, NOTEQUAL, EQUAL, GREATTHAN, GREATTHANOREQ }

	public static void relOp() {
		
		switch(lookahead.type) {
			case EQUAL :
				derivationSequence.add("<relOp> ::= ==");
				match(TokenType.EQUAL);
				break;
			case NOTEQUAL :
				derivationSequence.add("<relOp> ::= <>");
				match(TokenType.NOTEQUAL);
				break;
			case LESSTHAN :
				derivationSequence.add("<relOp> ::= <");
				match(TokenType.LESSTHAN);
				break;
			case GREATTHAN :
				derivationSequence.add("<relOp> ::= >");
				match(TokenType.GREATTHAN);
				break;
			case LESSTHANOREQ :
				derivationSequence.add("<relOp> ::= <=");
				match(TokenType.LESSTHANOREQ );
				break;
			case GREATTHANOREQ :
				derivationSequence.add("<relOp> ::= >=");
				match(TokenType.GREATTHANOREQ);
				break;
			default :
				skipProductionError("relOp");			
		}		
	}
	
	//36.	<addOp> ::= + | - | or
	//36.	FIRST(<addOp>) = { ADD , SUBTRACT , OR }
	
	public static void addOp() {
		
		switch(lookahead.type) {
			case ADD :
				derivationSequence.add("<addOp> ::= +");
				match(TokenType.ADD);
				break;
			case SUBTRACT :
				derivationSequence.add("<addOp> ::= -");
				match(TokenType.SUBTRACT);
				break;
			case OR :
				derivationSequence.add("<addOp> ::= or");
				match(TokenType.OR);
				break;
			default :
				skipProductionError("addOp");			
		}		
	}	
	
	//37.	<multOp> ::= * | / | and
	//37.	FIRST(<multOp>) = { MULTIPLY , DIVIDE , AND } 
	
	public static void multOp() {
		
		switch(lookahead.type) {
			case MULTIPLY :
				derivationSequence.add("<multOp> ::= *");
				match(TokenType.MULTIPLY);
				break;
			case DIVIDE :
				derivationSequence.add("<multOp> ::= /");
				match(TokenType.DIVIDE);
				break;
			case AND :
				derivationSequence.add("<multOp> ::= and");
				match(TokenType.AND);
				break;
			default :
				skipProductionError("multOp");			
		}		
	}	

}
