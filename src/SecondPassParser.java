import java.util.ArrayList;


public class SecondPassParser {

	public static Token lookahead = new Token();

	public static void parse() {
		lookahead = LexicalAnalyzer.getNextToken();
		prog();
		match(TokenType.ENDOFFILE);		
	}
	
	public static void match(TokenType input) { 
		if(input == lookahead.type) {
			//Match success 
			lookahead = LexicalAnalyzer.getNextToken();
		} else {
			lookahead = LexicalAnalyzer.getNextToken();
			return;
		}
	}
	
	//error recovery
	public static void skipProductionError(String production) {
		while(lookahead.type != TokenType.SEMICOLON && lookahead.type != TokenType.ENDOFFILE) {
			lookahead = LexicalAnalyzer.getNextToken();
		}
		return;	
	}
			
	//NON TERMINAL FUNCTIONS
	
	//1.	<prog> ::= <classDecl><progBody>
	//1.	FIRST(<prog>) = { CLASS , PROGRAM }
	
	public static void prog() {
	
			switch(lookahead.type) {
				case CLASS :
				case PROGRAM :
					//<prog> ::= <classDecl><progBody>
					
					//SEMANTIC ACTIONS: enterScope global
					SemanticAnalyzer.enterScope(SemanticAnalyzer.globalSymbolTable);
					
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
	
		switch(lookahead.type) {
			case CLASS :
				//<classDecl> ::= class id {<varDeclFuncDefHead>}; <classDecl>
				match(TokenType.CLASS);
				
				//SEMANTIC ACTIONS: enterScope
				SemanticAnalyzer.enterScope(SemanticAnalyzer.getTableReference(lookahead.lexeme));
				
				match(TokenType.IDENTIFIER);
				match(TokenType.OPENCURLYPAR);
				varDeclFuncDefHead();
				match(TokenType.CLOSECURLYPAR);
				match(TokenType.SEMICOLON);
				
				//SEMANTIC ACTIONS: exit scope
				SemanticAnalyzer.exitScope();
				
				classDecl();
				break;
			case PROGRAM :
				//<classDecl> ::= EPSILON
				break;
			default :
				skipProductionError("classDecl");			
		}		
	}
	
	//3.	<varDeclFuncDefHead> ::= <type> id <varDeclFuncDefTail> | e
	//3.	FIRST(<varDeclFuncDefHead>) = { FLOAT, IDENTIFIER, INT, EPSILON } 
	//2.	FOLLOW(<varDeclFuncDefHead>) = { CLOSECURLYPAR }
	
	public static void varDeclFuncDefHead() {
		
		switch(lookahead.type) {
			case INT :
			case FLOAT :
			case IDENTIFIER :
				//<varDeclFuncDefHead> ::= <type> id <varDeclFuncDefTail>
				type();
				
				String scopeName = lookahead.lexeme; //used to get function reference table
				
				match(TokenType.IDENTIFIER);
				varDeclFuncDefTail(scopeName);
				break;
			case CLOSECURLYPAR :
				//<varDeclFuncDefHead> ::= EPSILON
				break;
			default :
				skipProductionError("varDeclFuncDefHead");			
		}		
	}	
	
	//4.	<varDeclFuncDefTail> ::= <arraySize>;< varDeclFuncDefHead > | (<fParams>)<funcBody>;<funcDef>
	//4.	FIRST(<varDeclFuncDefTail>) = { OPENSQUAREPAR, SEMICOLON, OPENPAR } 
	 
	public static void varDeclFuncDefTail(String scopeName) {
		
		switch(lookahead.type) {
			case OPENSQUAREPAR :
			case SEMICOLON :
				//<varDeclFuncDefTail> ::= <arraySize>;< varDeclFuncDefHead >
				arraySize();
				match(TokenType.SEMICOLON);
				varDeclFuncDefHead();
				break;
			case OPENPAR :
				//<varDeclFuncDefTail> ::= (<fparams>)<funcBody>;<funcDef>
				
				//SEMANTIC ACTIONS: enterScope
				SemanticRecord funcMatch = SemanticAnalyzer.search(scopeName, SemanticAnalyzer.currentSymbolTable);
				if(LexicalAnalyzer.lexicalParseSuccess && FirstPassParser.syntaxParseSuccess && SemanticAnalyzer.semanticParseSuccess) {
					CodeGenerator.cGenFunctionRes(funcMatch);	
				}
				SemanticAnalyzer.enterScope(SemanticAnalyzer.getTableReference(scopeName));
				
				match(TokenType.OPENPAR);
				fParams();
				match(TokenType.CLOSEPAR);
				
				if(LexicalAnalyzer.lexicalParseSuccess && FirstPassParser.syntaxParseSuccess && SemanticAnalyzer.semanticParseSuccess) {
						CodeGenerator.cGenFunctionStart(funcMatch);
				}
				
				funcBody();
				
				match(TokenType.SEMICOLON);
								
				//SEMANTIC ACTIONS: exit scope
				SemanticAnalyzer.exitScope();
				
				funcDef();
				break;
			default :
				skipProductionError("varDeclFuncDefHead");			
		}		
	}
	
	//5.	<progBody> ::= program<funcBody>;<funcDef>
	//5.	FIRST(<progBody>) = { PROGRAM }
	
	public static void progBody() {
		
		switch(lookahead.type) {
			case PROGRAM :
				//<progBody> ::= program<funcBody>;<funcDef>
				match(TokenType.PROGRAM);
				
				//SEMANTIC ACTIONS: enterScope
				SemanticAnalyzer.enterScope(SemanticAnalyzer.getTableReference("program"));
				
				if(LexicalAnalyzer.lexicalParseSuccess && FirstPassParser.syntaxParseSuccess && SemanticAnalyzer.semanticParseSuccess) {
					CodeGenerator.cGen("entry");	
				}
				
				funcBody();
				match(TokenType.SEMICOLON);
				
				//SEMANTIC ACTIONS: exit scope
				SemanticAnalyzer.exitScope();
				
				if(LexicalAnalyzer.lexicalParseSuccess && FirstPassParser.syntaxParseSuccess && SemanticAnalyzer.semanticParseSuccess) {
					CodeGenerator.cGen("hlt");	
				}
				
				funcDef();			
				break;
			default :
				skipProductionError("progBody");				
		}		
			
	}
	
	//6.	<funcHead> ::= <type>id(<fParams>)
	//6.	FIRST(<funcHead>) = { INT , FLOAT , IDENTIFIER }
	
	public static void funcHead() {
		
		switch(lookahead.type) {
			case INT :
			case FLOAT :
			case IDENTIFIER :
				//<funcHead> ::= <type>id(<fParams>)
				type();
				
				//SEMANTIC ACTIONS: enterScope

				SemanticRecord funcMatch = SemanticAnalyzer.search(lookahead.lexeme, SemanticAnalyzer.currentSymbolTable);
				if(LexicalAnalyzer.lexicalParseSuccess && FirstPassParser.syntaxParseSuccess && SemanticAnalyzer.semanticParseSuccess) {
					CodeGenerator.cGenFunctionRes(funcMatch);
				}
				
				SemanticAnalyzer.enterScope(SemanticAnalyzer.getTableReference(lookahead.lexeme));
				
				match(TokenType.IDENTIFIER);
				match(TokenType.OPENPAR);
				fParams();
				match(TokenType.CLOSEPAR);
				
				if(LexicalAnalyzer.lexicalParseSuccess && FirstPassParser.syntaxParseSuccess && SemanticAnalyzer.semanticParseSuccess) {
					CodeGenerator.cGenFunctionStart(funcMatch);
				}
				
				break;
			default :
				skipProductionError("funcHead");			
		}		
			
	}
	
	//7.	<funcDef> ::= <funcHead><funcBody>; <funcDef> | e
	//7.	FIRST(<funcDef>) = { INT , FLOAT , IDENTIFIER , EPSILON }
	//3.	FOLLOW(<funcDef>) = { CLOSECURLYPAR , ENDOFFILE }
	
	public static void funcDef() {
		
		switch(lookahead.type) {
			case INT :
			case FLOAT :
			case IDENTIFIER :
				//<funcDef> ::= <funcHead><funcBody>; <funcDef>	
				funcHead();
				funcBody();
				match(TokenType.SEMICOLON);
				
				//SEMANTIC ACTIONS: exit scope
				SemanticAnalyzer.exitScope();
				
				funcDef();		
				break;
			case CLOSECURLYPAR :
			case ENDOFFILE :
				//<funcDef> ::= EPSILON
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
				//<funcBody> ::= {<varDeclStatementHead>}
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
		SemanticRecord sVarDeclStatementHead = new SemanticRecord();
		SemanticRecord sRelExpr = new SemanticRecord();
		SemanticRecord sExpr = new SemanticRecord();
		SemanticRecord sExpr2 = new SemanticRecord();
		SemanticRecord sVariable = new SemanticRecord();
		
		switch(lookahead.type) {
			case INT :
				//<varDeclStatementHead> ::= int id<arraySize>; <varDeclStatementHead>
				match(TokenType.INT);
				
				//SEMANTIC ACTIONS
				sVarDeclStatementHead = SemanticAnalyzer.search(lookahead.lexeme, SemanticAnalyzer.currentSymbolTable);
				if(LexicalAnalyzer.lexicalParseSuccess && FirstPassParser.syntaxParseSuccess && SemanticAnalyzer.semanticParseSuccess) {
					CodeGenerator.cGenResMem(sVarDeclStatementHead);	
				}
				
				match(TokenType.IDENTIFIER);
				arraySize();
				match(TokenType.SEMICOLON);
				varDeclStatementHead();
				break;
			case FLOAT :
				//<varDeclStatementHead> ::= float id<arraySize>; <varDeclStatementHead>
				match(TokenType.FLOAT);
				
				//SEMANTIC ACTIONS
				sVarDeclStatementHead = SemanticAnalyzer.search(lookahead.lexeme, SemanticAnalyzer.currentSymbolTable);
				if(LexicalAnalyzer.lexicalParseSuccess && FirstPassParser.syntaxParseSuccess && SemanticAnalyzer.semanticParseSuccess) {
					CodeGenerator.cGenResMem(sVarDeclStatementHead);	
				}
				
				
				
				match(TokenType.IDENTIFIER);
				arraySize();
				match(TokenType.SEMICOLON);
				varDeclStatementHead();			
				break;
			case IF :
				//<varDeclStatementHead> ::= if(<expr>)then<statBlock>else<statBlock>; <statement>
				match(TokenType.IF);
				match(TokenType.OPENPAR);
				expr(sExpr);
				match(TokenType.CLOSEPAR);
				
				//semantic translation
				String elseLabel = "else" + CodeGenerator.uniqueNumber;
				String endIfLabel = "endif" + CodeGenerator.uniqueNumber;
				CodeGenerator.updateTempAddress();
				if(LexicalAnalyzer.lexicalParseSuccess && FirstPassParser.syntaxParseSuccess && SemanticAnalyzer.semanticParseSuccess) {
					CodeGenerator.cGenBranch(sExpr,elseLabel);	
				}
				
				match(TokenType.THEN);
				statBlock();
				
				//semantic translation
				if(LexicalAnalyzer.lexicalParseSuccess && FirstPassParser.syntaxParseSuccess && SemanticAnalyzer.semanticParseSuccess) {
					CodeGenerator.cGen("j " + endIfLabel);	
				}
				
				match(TokenType.ELSE);
				
				//semantic translation
				if(LexicalAnalyzer.lexicalParseSuccess && FirstPassParser.syntaxParseSuccess && SemanticAnalyzer.semanticParseSuccess) {
					CodeGenerator.cGen(elseLabel);	
				}
				
				statBlock();
				
				//semantic translation
				if(LexicalAnalyzer.lexicalParseSuccess && FirstPassParser.syntaxParseSuccess && SemanticAnalyzer.semanticParseSuccess) {
					CodeGenerator.cGen(endIfLabel);	
				}
				
				
				match(TokenType.SEMICOLON);
				statement();
				break;
			case FOR :
				//<varDeclStatementHead> ::= for(<type>id<assignOp><expr>;<relExpr>;<assignStat>)<statBlock>; <statement>
				match(TokenType.FOR);
				match(TokenType.OPENPAR);
				type();
				
				//SEMANTIC ACTIONS
				sVarDeclStatementHead = SemanticAnalyzer.search(lookahead.lexeme, SemanticAnalyzer.currentSymbolTable);
				if(LexicalAnalyzer.lexicalParseSuccess && FirstPassParser.syntaxParseSuccess && SemanticAnalyzer.semanticParseSuccess) {
					CodeGenerator.cGenResMem(sVarDeclStatementHead);	
				}
				
				
				match(TokenType.IDENTIFIER);
				assignOp();
				expr(sExpr);
				
				//semantic translation initialization
				SemanticAnalyzer.checkTypeMatch(sVarDeclStatementHead, sExpr);
				if(LexicalAnalyzer.lexicalParseSuccess && FirstPassParser.syntaxParseSuccess && SemanticAnalyzer.semanticParseSuccess) {
					CodeGenerator.cGenAssign(sVarDeclStatementHead, sExpr);	
				}
				
				
				match(TokenType.SEMICOLON);
				
				//semantic translation loop
				String goWhileLabel = "gowhile" + CodeGenerator.uniqueNumber;
				String endWhileLabel = "endwhile" + CodeGenerator.uniqueNumber;
				CodeGenerator.updateTempAddress();
				if(LexicalAnalyzer.lexicalParseSuccess && FirstPassParser.syntaxParseSuccess && SemanticAnalyzer.semanticParseSuccess) {
					CodeGenerator.cGen(goWhileLabel);	
				}
				
				
				
				relExpr(sRelExpr);
				
				//semantic translation loop
				if(LexicalAnalyzer.lexicalParseSuccess && FirstPassParser.syntaxParseSuccess && SemanticAnalyzer.semanticParseSuccess) {
					CodeGenerator.cGenLoop(sRelExpr, endWhileLabel);		
				}
			
				
				match(TokenType.SEMICOLON);
				assignStat(sVariable,sExpr2);
				

				
				match(TokenType.CLOSEPAR);
				statBlock();
				
				//semantic translation
				SemanticAnalyzer.checkTypeMatch(sVariable, sExpr2); 
				if(LexicalAnalyzer.lexicalParseSuccess && FirstPassParser.syntaxParseSuccess && SemanticAnalyzer.semanticParseSuccess) {
					CodeGenerator.cGenAssign(sVariable, sExpr2);
					CodeGenerator.cGen("j " + goWhileLabel);
					CodeGenerator.cGen(endWhileLabel);					
				}

				
				match(TokenType.SEMICOLON);
				statement();
				break;
			case GET :
				//<varDeclStatementHead> ::= get(<variable>); <statement>
				match(TokenType.GET);
				match(TokenType.OPENPAR);
				variable(sVariable);
				match(TokenType.CLOSEPAR);
				
				//semantic translation 
				if(LexicalAnalyzer.lexicalParseSuccess && FirstPassParser.syntaxParseSuccess && SemanticAnalyzer.semanticParseSuccess) {
					CodeGenerator.cGenGet(sVariable);	
				}
				
				
				match(TokenType.SEMICOLON);
				statement();
				break;
			case PUT :
				//<varDeclStatementHead> ::= put(<expr>); <statement>
				match(TokenType.PUT);
				match(TokenType.OPENPAR);
				expr(sExpr);
				match(TokenType.CLOSEPAR);
				
				//semantic translation
				if(LexicalAnalyzer.lexicalParseSuccess && FirstPassParser.syntaxParseSuccess && SemanticAnalyzer.semanticParseSuccess) {
					CodeGenerator.cGenPut(sExpr);	
				}
				
				
				match(TokenType.SEMICOLON);
				statement();				
				break;
			case RETURN :
				//<varDeclStatementHead> ::= return(<expr>); <statement>
				match(TokenType.RETURN);
				match(TokenType.OPENPAR);
				expr(sExpr);
				
				SemanticRecord funcMatch = SemanticAnalyzer.search(SemanticAnalyzer.currentSymbolTable.name, SemanticAnalyzer.currentSymbolTable.parentSymbolTable);
				SemanticAnalyzer.checkTypeMatch(funcMatch, sExpr);  //check return type matches function
		
				//semantic translation
				if(LexicalAnalyzer.lexicalParseSuccess && FirstPassParser.syntaxParseSuccess && SemanticAnalyzer.semanticParseSuccess) {
					CodeGenerator.cGenFunctionEnd(funcMatch, sExpr);
				}
				
				match(TokenType.CLOSEPAR);
				match(TokenType.SEMICOLON);
				statement();
				break;
			case IDENTIFIER :
				//<varDeclStatementHead> ::= id <varDeclStatementTail>
				
				String idNameMatch = lookahead.lexeme; //needed for searching if variable assign statement or data type defined in declaration
				
				match(TokenType.IDENTIFIER);
				varDeclStatementTail(idNameMatch);
				break;
			case CLOSECURLYPAR :
				//<varDeclStatementHead> ::= EPSILON
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
	
	public static void varDeclStatementTail(String idNameMatch) {
		SemanticRecord sVarDeclStatementTail = new SemanticRecord();
		SemanticRecord sExpr = new SemanticRecord();
		
		switch(lookahead.type) {
			case IDENTIFIER :
				//<varDeclStatementTail> ::= id <arraySize>; <varDeclStatementHead>
				
				//SEMANTIC ACTIONS
				SemanticAnalyzer.checkDataTypeDefined(idNameMatch);
				sVarDeclStatementTail = SemanticAnalyzer.search(lookahead.lexeme, SemanticAnalyzer.currentSymbolTable);
				if(LexicalAnalyzer.lexicalParseSuccess && FirstPassParser.syntaxParseSuccess && SemanticAnalyzer.semanticParseSuccess) {
					CodeGenerator.cGenResMem(sVarDeclStatementTail);	
				}
				
				
				match(TokenType.IDENTIFIER);
				arraySize();
				match(TokenType.SEMICOLON);
				varDeclStatementHead();
				break;
			case OPENSQUAREPAR :
			case PERIOD :
			case ASSIGNMENT :
				//<varDeclStatementTail> ::= <indice><idnest><assignOp><expr> ; <statement>
				
				//SEMANTIC ACTIONS
				sVarDeclStatementTail = SemanticAnalyzer.search(idNameMatch, SemanticAnalyzer.currentSymbolTable);
						
				indice(sVarDeclStatementTail);
				idnest(sVarDeclStatementTail);
				assignOp();
				expr(sExpr);
				
				
				//semantic verification
				SemanticAnalyzer.checkTypeMatch(sVarDeclStatementTail, sExpr);
				SemanticAnalyzer.checkDimensionMatch(sVarDeclStatementTail);
				
				//semantic translation
				if(LexicalAnalyzer.lexicalParseSuccess && FirstPassParser.syntaxParseSuccess && SemanticAnalyzer.semanticParseSuccess) {
					CodeGenerator.cGenAssign(sVarDeclStatementTail, sExpr);	
				}
				
				
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
		SemanticRecord sStatement = new SemanticRecord();
		SemanticRecord sRelExpr = new SemanticRecord();
		SemanticRecord sExpr = new SemanticRecord();
		SemanticRecord sExpr2 = new SemanticRecord();
		SemanticRecord sVariable = new SemanticRecord();
		
		
		switch(lookahead.type) {
			case IDENTIFIER :
				//<statement> ::= <assignStat>; <statement>
				assignStat(sVariable, sExpr);
				
				SemanticAnalyzer.checkTypeMatch(sVariable, sExpr);
				if(LexicalAnalyzer.lexicalParseSuccess && FirstPassParser.syntaxParseSuccess && SemanticAnalyzer.semanticParseSuccess) {
					CodeGenerator.cGenAssign(sVariable, sExpr);	
				}
				
				
				match(TokenType.SEMICOLON);
				statement();
				break;
			case IF :
				//<statement> ::= if(<expr>)then<statBlock>else<statBlock>; <statement>
				match(TokenType.IF);								
				match(TokenType.OPENPAR);
				expr(sExpr);
				match(TokenType.CLOSEPAR);
				
				//semantic translation
				String elseLabel = "else" + CodeGenerator.uniqueNumber;
				String endIfLabel = "endif" + CodeGenerator.uniqueNumber;
				CodeGenerator.updateTempAddress();
				
				if(LexicalAnalyzer.lexicalParseSuccess && FirstPassParser.syntaxParseSuccess && SemanticAnalyzer.semanticParseSuccess) {
					CodeGenerator.cGenBranch(sExpr,elseLabel);	
				}
				
				
				match(TokenType.THEN);
				statBlock();
				
				//semantic translation
				if(LexicalAnalyzer.lexicalParseSuccess && FirstPassParser.syntaxParseSuccess && SemanticAnalyzer.semanticParseSuccess) {
					CodeGenerator.cGen("j " + endIfLabel);	
				}
				
				
				match(TokenType.ELSE);
				
				//semantic translation
				if(LexicalAnalyzer.lexicalParseSuccess && FirstPassParser.syntaxParseSuccess && SemanticAnalyzer.semanticParseSuccess) {
					CodeGenerator.cGen(elseLabel);	
				}
				
				statBlock();
				
				//semantic translation
				if(LexicalAnalyzer.lexicalParseSuccess && FirstPassParser.syntaxParseSuccess && SemanticAnalyzer.semanticParseSuccess) {
					CodeGenerator.cGen(endIfLabel);	
				}
				
				
				match(TokenType.SEMICOLON);
				statement();
				break;
			case FOR :
				//<varDeclStatementHead> ::= for(<type>id<assignOp><expr>;<relExpr>;<assignStat>)<statBlock>; <statement>
				match(TokenType.FOR);
				match(TokenType.OPENPAR);
				type();
				
				//SEMANTIC ACTIONS
				sStatement = SemanticAnalyzer.search(lookahead.lexeme, SemanticAnalyzer.currentSymbolTable);
				if(LexicalAnalyzer.lexicalParseSuccess && FirstPassParser.syntaxParseSuccess && SemanticAnalyzer.semanticParseSuccess) {
					CodeGenerator.cGenResMem(sStatement);	
				}
				
				
				match(TokenType.IDENTIFIER);
				assignOp();
				expr(sExpr);
				
				//semantic translation initialization
				SemanticAnalyzer.checkTypeMatch(sStatement, sExpr);
				if(LexicalAnalyzer.lexicalParseSuccess && FirstPassParser.syntaxParseSuccess && SemanticAnalyzer.semanticParseSuccess) {
					CodeGenerator.cGenAssign(sStatement, sExpr);	
				}
				
				
				match(TokenType.SEMICOLON);
				
				//semantic translation loop
				String goWhileLabel = "gowhile" + CodeGenerator.uniqueNumber;
				String endWhileLabel = "endwhile" + CodeGenerator.uniqueNumber;
				CodeGenerator.updateTempAddress();
				if(LexicalAnalyzer.lexicalParseSuccess && FirstPassParser.syntaxParseSuccess && SemanticAnalyzer.semanticParseSuccess) {
					CodeGenerator.cGen(goWhileLabel);	
				}
				
				
				
				relExpr(sRelExpr);
				
				//semantic translation loop
				if(LexicalAnalyzer.lexicalParseSuccess && FirstPassParser.syntaxParseSuccess && SemanticAnalyzer.semanticParseSuccess) {
					CodeGenerator.cGenLoop(sRelExpr, endWhileLabel);	
				}
				
				
				match(TokenType.SEMICOLON);
				assignStat(sVariable,sExpr2); 
						
				match(TokenType.CLOSEPAR);
				statBlock();
				
				//semantic translation
				SemanticAnalyzer.checkTypeMatch(sVariable, sExpr2);
				if(LexicalAnalyzer.lexicalParseSuccess && FirstPassParser.syntaxParseSuccess && SemanticAnalyzer.semanticParseSuccess) {
					CodeGenerator.cGenAssign(sVariable, sExpr2);
					CodeGenerator.cGen("j " + goWhileLabel);
					CodeGenerator.cGen(endWhileLabel);
										
				}

				match(TokenType.SEMICOLON);
				statement();
				break;
			case GET :
				//<statement> ::= get(<variable>); <statement>
				match(TokenType.GET);
				match(TokenType.OPENPAR);
				variable(sVariable);
				match(TokenType.CLOSEPAR);
				
				//semantic translation 
				if(LexicalAnalyzer.lexicalParseSuccess && FirstPassParser.syntaxParseSuccess && SemanticAnalyzer.semanticParseSuccess) {
					CodeGenerator.cGenGet(sVariable);
				}
				
				
				match(TokenType.SEMICOLON);
				statement();
				break;
			case PUT :
				//<statement> ::= put(<expr>); <statement>
				match(TokenType.PUT);
				match(TokenType.OPENPAR);
				expr(sExpr);
				match(TokenType.CLOSEPAR);
				
				//semantic translation
				if(LexicalAnalyzer.lexicalParseSuccess && FirstPassParser.syntaxParseSuccess && SemanticAnalyzer.semanticParseSuccess) {
					CodeGenerator.cGenPut(sExpr);	
				}
				
				
				match(TokenType.SEMICOLON);
				statement();				
				break;
			case RETURN :
				//<statement> ::= return(<expr>); <statement>
				match(TokenType.RETURN);
				match(TokenType.OPENPAR);
				expr(sExpr);
				
				SemanticRecord funcMatch = SemanticAnalyzer.search(SemanticAnalyzer.currentSymbolTable.name, SemanticAnalyzer.currentSymbolTable.parentSymbolTable);
				SemanticAnalyzer.checkTypeMatch(funcMatch, sExpr);  //check return type matches function
				
				//semantic translation
				if(LexicalAnalyzer.lexicalParseSuccess && FirstPassParser.syntaxParseSuccess && SemanticAnalyzer.semanticParseSuccess) {
					CodeGenerator.cGenFunctionEnd(funcMatch, sExpr);
				}
				
				
				match(TokenType.CLOSEPAR);
				match(TokenType.SEMICOLON);
				statement();
				break;
			case CLOSECURLYPAR :
				//<statement> ::= EPSILON
				break;
			default :
				skipProductionError("statement");			
		}	
	}	
	
	//12.	<assignStat> ::= <variable><assignOp><expr>
	//12.	FIRST(<assignStat>) = { IDENTIFIER }
	
	public static void assignStat(SemanticRecord sVariable, SemanticRecord sExpr) {
		
		switch(lookahead.type) {
			case IDENTIFIER :
				//<assignStat> ::= <variable><assignOp><expr>
				variable(sVariable);
				assignOp();
				expr(sExpr);
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
				//<statBlock> ::= {<statement>}
				match(TokenType.OPENCURLYPAR);
				statement();
				match(TokenType.CLOSECURLYPAR);
				break;
			case SEMICOLON :
			case ELSE :
				//<statBlock> ::= EPSILON
				break;
			default :
				skipProductionError("statBlock");			
		}	
	}
	
	//14.	<expr> ::= <arithExpr><exprTail>
	//14.	FIRST(<expr>) = { IDENTIFIER , NUMINT , NUMFLOAT , OPENPAR , NOT , ADD , SUBTRACT }
	
	public static void expr(SemanticRecord sExpr) {
		
		SemanticRecord sArithExpr = new SemanticRecord(), sExprTail = new SemanticRecord();
		
		switch(lookahead.type) {
			case IDENTIFIER :
			case NUMINT :
			case NUMFLOAT :
			case OPENPAR :
			case NOT :
			case ADD :
			case SUBTRACT :
				//<expr> ::= <arithExpr><exprTail>
				arithExpr(sArithExpr);
				exprTail(sArithExpr, sExprTail); //exprTail inherits from arithExpr
				
				//SEMANTIC ACTIONS
				sExpr.copyAttributes(sExprTail); //synthesized attributes sent up
				
				break;
			default :
				skipProductionError("expr");			
		}		
	}	
	
	//15.	<exprTail> ::= <relOp><arithExpr> | e 
	//15.	FIRST(<exprTail>) = { LESSTHAN, LESSTHANOREQ, NOTEQUAL, EQUAL, GREATTHAN, GREATTHANOREQ, EPSILON } 
	//7.	FOLLOW(<exprTail>) = { SEMICOLON, CLOSEPAR, COMMA }
	
	public static void exprTail(SemanticRecord iArithExpr, SemanticRecord sExprTail) {
		SemanticRecord sArithExpr = new SemanticRecord();
		
		switch(lookahead.type) {
			case LESSTHAN :
			case LESSTHANOREQ :
			case NOTEQUAL :
			case EQUAL :
			case GREATTHAN :
			case GREATTHANOREQ :
				//<exprTail> ::= <relOp><arithExpr>
				TokenType relOp = lookahead.type;
				
				relOp();
				arithExpr(sArithExpr);
				
				//SEMANTIC ACTIONS
				
				if(LexicalAnalyzer.lexicalParseSuccess && FirstPassParser.syntaxParseSuccess && SemanticAnalyzer.semanticParseSuccess) {
					SemanticAnalyzer.checkTypeMatch(iArithExpr, sArithExpr);
					CodeGenerator.cGenExpr(iArithExpr, sArithExpr, relOp);	
				}
				
				sArithExpr.setMemoryAddress(CodeGenerator.tempAddress);
				sArithExpr.setIdKind("variable"); //temp result treated as variable after num literals occur for CG 
				CodeGenerator.updateTempAddress();
				sExprTail.copyAttributes(sArithExpr); //synthesized attributes sent up
				
				break;
			case SEMICOLON :
			case CLOSEPAR :
			case COMMA :
				//<exprTail> ::= EPSILON
				
				//SEMANTIC ACTIONS
				sExprTail.copyAttributes(iArithExpr); //inherited attribute sent up
				
				break;
			default :
				skipProductionError("exprTail");			
		}		
	}
	
	//16.	<relExpr> ::= <arithExpr><relOp><arithExpr>
	//16.	FIRST(<relExpr>) = { IDENTIFIER , NUMINT , NUMFLOAT , OPENPAR , NOT , ADD , SUBTRACT } 
	
	public static void relExpr(SemanticRecord sRelExpr) {
		SemanticRecord sArithExpr1 = new SemanticRecord(), sArithExpr2 = new SemanticRecord();
		
		switch(lookahead.type) {
			case IDENTIFIER :
			case NUMINT :
			case NUMFLOAT :
			case OPENPAR :
			case NOT :
			case ADD :
			case SUBTRACT :
				//<relExpr> ::= <arithExpr><relOp><arithExpr>
				arithExpr(sArithExpr1);
				
				TokenType relOp = lookahead.type;
				
				relOp();
				arithExpr(sArithExpr2);
				
				//SEMANTIC ACTIONS
				
				if(LexicalAnalyzer.lexicalParseSuccess && FirstPassParser.syntaxParseSuccess && SemanticAnalyzer.semanticParseSuccess) {
					SemanticAnalyzer.checkTypeMatch(sArithExpr1, sArithExpr2);
					CodeGenerator.cGenExpr(sArithExpr1, sArithExpr2, relOp);	
				}
				
				sArithExpr2.setMemoryAddress(CodeGenerator.tempAddress);
				sArithExpr2.setIdKind("variable"); //temp result treated as variable after num literals occur for CG 
				CodeGenerator.updateTempAddress();
				sRelExpr.copyAttributes(sArithExpr2); //synthesized attributes sent up
				
				break;
			default :
				skipProductionError("relExpr");			
		}		
	}
	
	//17.	<arithExpr> ::= <term><arithExprTail>
	//17.	FIRST(<arithExpr>) = { IDENTIFIER , NUMINT , NUMFLOAT , OPENPAR , NOT , ADD , SUBTRACT } 
	
	public static void arithExpr(SemanticRecord sArithExpr) {
		SemanticRecord sTerm = new SemanticRecord(), sArithExprTail = new SemanticRecord(); 
		
		switch(lookahead.type) {
			case IDENTIFIER :
			case NUMINT :
			case NUMFLOAT :
			case OPENPAR :
			case NOT :
			case ADD :
			case SUBTRACT :
				//<arithExpr> ::= <term><arithExprTail>
				term(sTerm);
				arithExprTail(sTerm, sArithExprTail);
				
				//semantic actions
				sArithExpr.copyAttributes(sArithExprTail); //synthesized attributes sent up
				
				
				break;
			default :
				skipProductionError("arithExpr");			
		}		
	}
	
	//18.	<arithExprTail> ::= <addOp><term><arithExprTail> | e
	//18.	FIRST<arithExprTail>) = { ADD, SUBTRACT, OR, EPSILON } 
	//8.	FOLLOW(<arithExprTail>) = { SEMICOLON, CLOSEPAR, COMMA, LESSTHAN, LESSTHANOREQ, NOTEQUAL, EQUAL, GREATTHAN, GREATTHANOREQ, CLOSESQUAREPAR } 
	
	public static void arithExprTail(SemanticRecord iTerm, SemanticRecord sArithExprTail) {
		
		SemanticRecord sTerm = new SemanticRecord(), sArithExprTail2 = new SemanticRecord();
		
		switch(lookahead.type) {
			case ADD :
			case SUBTRACT :
			case OR :
				//<arithExprTail> ::= <addOp><term><arithExprTail>
				
				TokenType addOp = lookahead.type; //pass operator to code generator
				
				addOp();
				term(sTerm);
				arithExprTail(sTerm, sArithExprTail2);
				
				
				if(LexicalAnalyzer.lexicalParseSuccess && FirstPassParser.syntaxParseSuccess && SemanticAnalyzer.semanticParseSuccess) {
					SemanticAnalyzer.checkTypeMatch(iTerm, sArithExprTail2);
					CodeGenerator.cGenExpr(iTerm, sArithExprTail2, addOp);	
				}
				
				sArithExprTail2.setMemoryAddress(CodeGenerator.tempAddress);
				sArithExprTail2.setIdKind("variable"); //temp result treated as variable after num literals occur for CG 
				CodeGenerator.updateTempAddress();
				sArithExprTail.copyAttributes(sArithExprTail2); //synthesized attributes sent up
				
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
				//<arithExprTail> ::= EPSILON
				
				//semantic actions
				sArithExprTail.copyAttributes(iTerm); //inherited attribute sent up
				
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
				//<sign> ::= +
				match(TokenType.ADD);
				break;
			case SUBTRACT :
				//<sign> ::= -
				match(TokenType.SUBTRACT);
				break;
			default :
				skipProductionError("sign");			
		}		
	}	
	
	//20.	<term> ::= <factor><termTail>
	//20.	FIRST(<term>) = { IDENTIFIER , NUMINT , NUMFLOAT , OPENPAR , NOT , ADD , SUBTRACT }
	
	public static void term(SemanticRecord sTerm) {
		SemanticRecord sFactor = new SemanticRecord(), sTermTail = new SemanticRecord();
		
		switch(lookahead.type) {
			case IDENTIFIER :
			case NUMINT :
			case NUMFLOAT :
			case OPENPAR :
			case NOT :
			case ADD :
			case SUBTRACT :
				//<term> ::= <factor><termTail>
				
				factor(sFactor);
				termTail(sFactor, sTermTail); //term tail inherits from sFactor
				
				sTerm.copyAttributes(sTermTail); //synthesized attributes sent up
				
				break;
			default :
				skipProductionError("term");			
		}		
	}
	
	//21.	<termTail> ::= <multOp><factor><termTail> | e
	//21.	FIRST(<termTail>) = { MULTIPLY, DIVIDE, AND, EPSILON }
	//9.	FOLLOW(<termTail>) = { SEMICOLON, CLOSEPAR, COMMA, LESSTHAN, LESSTHANOREQ, NOTEQUAL, EQUAL, GREATTHAN, GREATTHANOREQ, CLOSESQUAREPAR, ADD, SUBTRACT, OR }
	
	public static void termTail(SemanticRecord iFactor, SemanticRecord sTermTail) {
		
		SemanticRecord sFactor = new SemanticRecord(), sTermTail2 = new SemanticRecord();
		
		switch(lookahead.type) {
			case MULTIPLY :
			case DIVIDE :
			case AND :
				//<termTail> ::= <multOp><factor><termTail>
				
				TokenType multOp = lookahead.type;
				
				multOp();
				factor(sFactor);
				termTail(sFactor, sTermTail2);
				
				//SEMANTIC ACTIONS
				
				if(LexicalAnalyzer.lexicalParseSuccess && FirstPassParser.syntaxParseSuccess && SemanticAnalyzer.semanticParseSuccess) {
					SemanticAnalyzer.checkTypeMatch(iFactor, sTermTail2);
					CodeGenerator.cGenExpr(iFactor, sTermTail2, multOp);	
				}
				
				sTermTail2.setMemoryAddress(CodeGenerator.tempAddress);
				sTermTail2.setIdKind("variable"); //temp result treated as variable after num literals occur for CG 
				CodeGenerator.updateTempAddress();
				sTermTail.copyAttributes(sTermTail2); //synthesized attributes sent up
				
				
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
				//<termTail> ::= EPSILON
		
				sTermTail.copyAttributes(iFactor); //inherited attributes sent up
						
				break;
			default :
				skipProductionError("termTail");			
		}		
	}	
	
	//22.	<factor> ::= id<factorTail>| numint | numfloat | (<arithExpr>) | not<factor> | <sign><factor>
	//22.	FIRST(<factor>) = { IDENTIFIER , NUMINT , NUMFLOAT , OPENPAR , NOT , ADD , SUBTRACT } 
	
	public static void factor(SemanticRecord sFactor) {
		SemanticRecord sArithExpr = new SemanticRecord();

		switch(lookahead.type) {
			case IDENTIFIER :
				//<factor> ::= id<factorTail>
				
				//SEMANTIC ACTIONS
				SemanticRecord sFactorMatch = SemanticAnalyzer.search(lookahead.lexeme,SemanticAnalyzer.currentSymbolTable);
				sFactor.copyAttributes(sFactorMatch);
												
				match(TokenType.IDENTIFIER);
				factorTail(sFactor);
				break;
			case NUMINT :
				//<factor> ::= numint
				
				int intValue = Integer.parseInt(lookahead.lexeme);
				sFactor.copyAttributes(new SemanticRecord("numliteral", "int", intValue));
				
				match(TokenType.NUMINT);
				break;
			case NUMFLOAT :
				//<factor> ::= numfloat
				

				float floatValue = Float.parseFloat(lookahead.lexeme);
				sFactor.copyAttributes(new SemanticRecord("numliteral", "float", floatValue));
				
				match(TokenType.NUMFLOAT);
				break;
			case OPENPAR :
				//<factor> ::= (<arithExpr>)
				match(TokenType.OPENPAR);
				
				arithExpr(sArithExpr);
				sFactor.copyAttributes(sArithExpr);
				
				match(TokenType.CLOSEPAR);
				break;
			case NOT :
				//<factor> ::= not<factor>
				match(TokenType.NOT);
				factor(sFactor);
				break;
			case ADD :
			case SUBTRACT :
				//<factor> ::= <sign><factor>
				sign();
				factor(sFactor);
				break;
			default :
				skipProductionError("factor");			
		}	
		
		
	}
	
	//23.	<factorTail> ::= <indice><factornest>|(<aParams>)
	//23.	FIRST(<factorTail>) = { OPENSQUAREPAR, PERIOD, OPENPAR, EPSILON }	
	//10.	FOLLOW(<factorTail>) = { SEMICOLON, CLOSEPAR, COMMA, LESSTHAN, LESSTHANOREQ, NOTEQUAL, EQUAL, GREATTHAN, GREATTHANOREQ, CLOSESQUAREPAR, ADD, SUBTRACT, OR, MULTIPLY, DIVIDE, AND }
	
	public static void factorTail(SemanticRecord iFactor) {
	
		
		switch(lookahead.type) {
			case OPENSQUAREPAR :
			case PERIOD :
				//<factorTail> ::= <indice><factornest>				
				indice(iFactor);
				
				//semantic verification
				SemanticAnalyzer.checkDimensionMatch(iFactor);
		
				
				factornest(iFactor);
				break;
			case OPENPAR :
				//<factorTail> ::= (<aParams>)
				match(TokenType.OPENPAR);
				
				aParams(iFactor);
				
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
				//<factorTail> ::= EPSILON
				break;
			default :
				skipProductionError("factorTail");			
		}		
	}
	
	//24.	<factornest> ::= . id <factorTail> | e
	//24.	FIRST(<factornest>) = { PERIOD, EPSILON }
	//11.	FOLLOW(<factornest>) = { SEMICOLON, CLOSEPAR, COMMA, LESSTHAN, LESSTHANOREQ, NOTEQUAL, EQUAL, GREATTHAN, GREATTHANOREQ, CLOSESQUAREPAR, ADD, SUBTRACT, OR, MULTIPLY, DIVIDE, AND }
	
	public static void factornest(SemanticRecord iFactor) {
		
		SemanticRecord sFactorNest = new SemanticRecord();
		
		switch(lookahead.type) {
			case PERIOD :
				//<factornest> ::= . id <factorTail>
				match(TokenType.PERIOD);
				
				sFactorNest =  SemanticAnalyzer.search(lookahead.lexeme, SemanticAnalyzer.getLocalClassTable(iFactor.idType));
				
				match(TokenType.IDENTIFIER);
				
				factorTail(sFactorNest);
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
				//<factornest> ::= EPSILON
				break;
			default :
				skipProductionError("factornest");			
		}		
	}
	
	//25.	<variable> ::= id<indice><idnest>
	//25.	FIRST(<variable>) = { IDENTIFIER }
	
	public static void variable(SemanticRecord sVariable) {
		
		switch(lookahead.type) {
			case IDENTIFIER :
				//<variable> ::= id<indice><idnest>
				
				//SEMANTIC ACTIONS
				SemanticRecord sVariableMatch = SemanticAnalyzer.search(lookahead.lexeme,SemanticAnalyzer.currentSymbolTable);
				sVariable.copyAttributes(sVariableMatch);
				
				match(TokenType.IDENTIFIER);
				indice(sVariable);
				
				//semantic verification
				SemanticAnalyzer.checkDimensionMatch(sVariable);
				
				idnest(sVariable);
				break;
			default :
				skipProductionError("variable");			
		}		
	}
	
	//26.	<idnest> ::= . id<indice><idnest> | e
	//26.	FIRST(<idnest>) = { PERIOD , EPSILON }
	//12.	FOLLOW(<idnest>) = { ASSIGNMENT, CLOSEPAR } 
	
	public static void idnest(SemanticRecord sVar) {
		
		switch(lookahead.type) {
			case PERIOD :
				//<idnest> ::= . id<indice><idnest>
				match(TokenType.PERIOD);
				match(TokenType.IDENTIFIER);
				indice(sVar);
				idnest(sVar);
				break;
			case ASSIGNMENT :
			case CLOSEPAR :
				//<idnest> ::= EPSILON
				break;
			default :
				skipProductionError("idnest");			
		}		
	}
	
	//27.	<indice> ::= [<arithExpr>] <indice> | e
	//27.	FIRST(<indice>) = { OPENSQUAREPAR , EPSILON }
	//13.	FOLLOW(<indice>) = { PERIOD, ASSIGNMENT, SEMICOLON, CLOSEPAR, COMMA, LESSTHAN, LESSTHANOREQ, NOTEQUAL, EQUAL, GREATTHAN, GREATTHANOREQ, CLOSESQUAREPAR, ADD, SUBTRACT, OR, MULTIPLY, DIVIDE, AND} 

	public static void indice(SemanticRecord sFactor) {
		
		SemanticRecord sArithExpr = new SemanticRecord();
		
		switch(lookahead.type) {
			case OPENSQUAREPAR :
				//<indice> ::= [<arithExpr>] <indice>
				
				//used for dimension checking
				sFactor.incDimensionCounter();
				
				match(TokenType.OPENSQUAREPAR);
				arithExpr(sArithExpr);
				match(TokenType.CLOSESQUAREPAR);
				indice(sFactor);
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
				//<indice> ::= EPSILON
				break;
			default :
				skipProductionError("indice");			
		}	
	}
	
	//28.	<arraySize> ::= [ numint ] <arraySize> | e
	//28.	FIRST(<arraySize>) = { OPENSQUAREPAR , EPSILON }
	//14.	FOLLOW(<arraySize>) = { SEMICOLON, CLOSEPAR, COMMA } 

	public static void arraySize() {
		
		switch(lookahead.type) {
			case OPENSQUAREPAR:
				//<arraySize> ::= [ numint ] <arraySize>
				match(TokenType.OPENSQUAREPAR);
				match(TokenType.NUMINT);
				match(TokenType.CLOSESQUAREPAR);
				arraySize();			
				break;
			case SEMICOLON :
			case CLOSEPAR :
			case COMMA :
				//<arraySize> ::= EPSILON
				break;
			default :
				skipProductionError("arraySize");			
		}		
	}
	
	//29.	<type> ::= int | float | id
	//29.	FIRST(<type>) = { INT , FLOAT , IDENTIFIER }
	
	public static void type() {
		
		switch(lookahead.type) {
			case INT :
				//<type> ::= int
				match(TokenType.INT);
				break;
			case FLOAT :
				//<type> ::= float
				match(TokenType.FLOAT);
				break;
			case IDENTIFIER :
				//<type> ::= id
				
				//SEMANTIC ACTIONS
				SemanticAnalyzer.checkDataTypeDefined(lookahead.lexeme);
				
				match(TokenType.IDENTIFIER);
				break;
			default :
				skipProductionError("type");			
		}		
	}	
	
	//30.	<fParams> ::= <type>id<arraySize><fParamsTail> | e
	//30.	FIRST(<fParams>) = { INT , FLOAT , ID , EPSILON}
	//15.	FOLLOW(<fParams>) = { CLOSEPAR }
	
	public static void fParams() {
		SemanticRecord sFParams;
		
		switch(lookahead.type) {
			case INT :
			case FLOAT :
			case IDENTIFIER :
				//<fParams> ::= <type>id<arraySize><fParamsTail>
				type();
				
				//SEMANTIC ACTIONS
				sFParams = SemanticAnalyzer.search(lookahead.lexeme, SemanticAnalyzer.currentSymbolTable);
				if(LexicalAnalyzer.lexicalParseSuccess && FirstPassParser.syntaxParseSuccess && SemanticAnalyzer.semanticParseSuccess) {
					CodeGenerator.cGenResMem(sFParams);	
				}
				
				match(TokenType.IDENTIFIER);
				arraySize();
				fParamsTail();
				break;
			case CLOSEPAR :
				//<fParams> ::= EPSILON
				break;
			default :
				skipProductionError("fParams");			
		}		
	}	
	
	//31.	<aParams> ::= <expr><aParamsTail> | e
	//31.	FIRST(<aParams) = { IDENTIFIER , NUMINT , NUMFLOAT , OPENPAR , NOT , ADD , SUBTRACT, EPSILON }
	//16.	FOLLOW(<aParams>) = { CLOSEPAR } 
	
	public static void aParams(SemanticRecord iFunc) {
		SemanticRecord sExpr = new SemanticRecord();
		ArrayList<SemanticRecord> argumentList = new ArrayList<SemanticRecord>();
		
		
		switch(lookahead.type) {
			case IDENTIFIER :
			case NUMINT :
			case NUMFLOAT :
			case OPENPAR :
			case NOT :
			case ADD :
			case SUBTRACT :
				//<aParams> ::= <expr><aParamsTail>
				expr(sExpr);
				
				//used for checking arguments match formal parameters
				argumentList.add(sExpr);
				
				aParamsTail(argumentList);
				
				//semantic verification
				SemanticAnalyzer.checkArgumentMatch(iFunc, argumentList);
				if(LexicalAnalyzer.lexicalParseSuccess && FirstPassParser.syntaxParseSuccess && SemanticAnalyzer.semanticParseSuccess) {
					CodeGenerator.cGenFunctionCall(iFunc, argumentList);
				}
				
				break;
			case CLOSEPAR :
				//<aParams> ::= EPSILON
				break;
			default :
				skipProductionError("aParams");			
		}		
	}	
	
	//32.	<fParamsTail> ::= ,<type>id<arraySize> <fParamsTail> | e
	//32.	FIRST(<fParamsTail>) = { COMMA , EPSILON } 
	//17.	FOLLOW(<fParamsTail>) = { CLOSEPAR }

	public static void fParamsTail() {
		SemanticRecord sFParamsTail;
		
		switch(lookahead.type) {
			case COMMA :
				//<fParamsTail> ::= ,<type>id<arraySize> <fParamsTail>
				match(TokenType.COMMA);
				type();
				
				//SEMANTIC ACTIONS
				
				sFParamsTail = SemanticAnalyzer.search(lookahead.lexeme, SemanticAnalyzer.currentSymbolTable);
				if(LexicalAnalyzer.lexicalParseSuccess && FirstPassParser.syntaxParseSuccess && SemanticAnalyzer.semanticParseSuccess) {
					CodeGenerator.cGenResMem(sFParamsTail);	
				}
				
				
				match(TokenType.IDENTIFIER);
				arraySize();
				fParamsTail();
				break;
			case CLOSEPAR :
				//<fParamsTail> ::= EPSILON
				break;
			default :
				skipProductionError("fParamsTail");			
		}		
	}
	
	//33.	<aParamsTail> ::= ,<expr> <aParamsTail> | e
	//33.	FIRST(<aParamsTail>) = { COMMA , EPSILON }
	//18.	FOLLOW( <aParamsTail>) = { CLOSEPAR }  
	
	public static void aParamsTail(ArrayList<SemanticRecord> argumentList) {
		SemanticRecord sExpr = new SemanticRecord();
		
		switch(lookahead.type) {
			case COMMA :
				//<aParamsTail> ::= ,<expr> <aParamsTail>
				match(TokenType.COMMA);
				expr(sExpr);
				
				argumentList.add(sExpr);
				
				aParamsTail(argumentList);
				break;
			case CLOSEPAR :
				//<aParamsTail> ::= EPSILON
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
				//<assignOp> ::= =");
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
				//<relOp> ::= ==
				match(TokenType.EQUAL);
				break;
			case NOTEQUAL :
				//<relOp> ::= <>
				match(TokenType.NOTEQUAL);
				break;
			case LESSTHAN :
				//<relOp> ::= <
				match(TokenType.LESSTHAN);
				break;
			case GREATTHAN :
				//<relOp> ::= >
				match(TokenType.GREATTHAN);
				break;
			case LESSTHANOREQ :
				//<relOp> ::= <=
				match(TokenType.LESSTHANOREQ );
				break;
			case GREATTHANOREQ :
				//<relOp> ::= >=
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
				//<addOp> ::= +
				match(TokenType.ADD);
				break;
			case SUBTRACT :
				//<addOp> ::= -
				match(TokenType.SUBTRACT);
				break;
			case OR :
				//<addOp> ::= or
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
				//<multOp> ::= *
				match(TokenType.MULTIPLY);
				break;
			case DIVIDE :
				//<multOp> ::= /
				match(TokenType.DIVIDE);
				break;
			case AND :
				//<multOp> ::= and
				match(TokenType.AND);
				break;
			default :
				skipProductionError("multOp");			
		}		
	}	

}
