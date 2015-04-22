import java.util.ArrayList;


public class LexicalAnalyzer {
	
	public static int lineCount = 1;
	public static int index = 0;
	public static String inputString;
	public static ArrayList<String> tokenSequence = new ArrayList<String>();
	
	//Lexical error reporting
	public static ArrayList<String> lexicalErrorList = new ArrayList<String>();
	public static Boolean lexicalParseSuccess = true;
	public static int lexicalErrorCount = 0;
	
	public LexicalAnalyzer(String inputString) {
		this.lineCount = 1;
		this.index = 0;
		this.inputString = inputString; 
	}
	
	
	public static Token getNextToken() {
		
		StringBuilder lexeme =  new StringBuilder();
		Token token = new Token();
		
		//consume whitespace 
		consumeWhitespace();

		//division, consume comments 
		consumeComment();
	
		//end of string
		if(index >= inputString.length()) { 
			token = new Token(TokenType.ENDOFFILE,null, lineCount);
		}	
		//numbers (integers , floats)
		else if(Character.isDigit(inputString.charAt(index))) {
			
			if(inputString.charAt(index) == '0') { 
				lexeme.append(inputString.charAt(index));
				index++;
				
				if (index < inputString.length() && Character.isDigit(inputString.charAt(index))) {
					//leading zero consume . and digits
					while(index < inputString.length() && Character.isDigit(inputString.charAt(index)) 
							||inputString.charAt(index) == '.') {
						lexeme.append(inputString.charAt(index));
						index++;
					}
					token = new Token(TokenType.ERRORLEADINGZERO,lexeme.toString(), lineCount);
					
				} else if (index < inputString.length() && inputString.charAt(index) == '.') {
					//process float
					lexeme.append(inputString.charAt(index));
					index++;
					while(index < inputString.length() && Character.isDigit(inputString.charAt(index))) {
						lexeme.append(inputString.charAt(index));
						index++;
					}
					token = new Token(TokenType.NUMFLOAT,lexeme.toString(), lineCount);
					
					
					if(inputString.charAt(index - 1) == '0' && inputString.charAt(index - 2) != '.' ) {
						//trailing zero
						token = new Token(TokenType.ERRORTRAILINGZERO,lexeme.toString(), lineCount);
					}
					
				} else {
					//lexeme = 0
					token = new Token(TokenType.NUMINT,lexeme.toString(), lineCount);
				}
			
			} else {
				//process integers
				while(index < inputString.length() && Character.isDigit(inputString.charAt(index))) {
					lexeme.append(inputString.charAt(index));
					index++;
				}
				token = new Token(TokenType.NUMINT,lexeme.toString(), lineCount);
				
				if(index < inputString.length() && inputString.charAt(index) == '.') {
					//process float
					lexeme.append(inputString.charAt(index));
					index++;
					while(index < inputString.length() && Character.isDigit(inputString.charAt(index))) {
						lexeme.append(inputString.charAt(index));
						index++;
					}
					token = new Token(TokenType.NUMFLOAT,lexeme.toString(), lineCount);
					
					if(inputString.charAt(index - 1) == '0' && inputString.charAt(index - 2) != '.' ) {
						//trailing zero
						token = new Token(TokenType.ERRORTRAILINGZERO,lexeme.toString(), lineCount);
					}
				}
			}
		}
	
		//identifier or reserved word
		else  if(Character.isLetter(inputString.charAt(index))){ 	
			TokenType type;
			
			while(index < inputString.length() && (Character.isLetterOrDigit(inputString.charAt(index)) 
					 || inputString.charAt(index) == '_')) {	  
				lexeme.append(inputString.charAt(index));
				index++;
				 
			}
			//determine token type (identifier or one of the reserved words)
			type = isReservedWord(lexeme.toString());
			
			token = new Token(type, lexeme.toString(), lineCount);	
		}
		
		//SINGLE CHARACTER TOKENS
		//semicolon
		else  if(inputString.charAt(index) == ';') { 
			lexeme.append(inputString.charAt(index));
			index++;
			token = new Token(TokenType.SEMICOLON, lexeme.toString(), lineCount);	
			
		}

		//comma
		else  if(inputString.charAt(index) == ',') { 
			lexeme.append(inputString.charAt(index));
			index++;
			token = new Token(TokenType.COMMA, lexeme.toString(), lineCount);	
			
		}
		
		//period
		else  if(inputString.charAt(index) == '.') { 
			lexeme.append(inputString.charAt(index));
			index++;
			token = new Token(TokenType.PERIOD, lexeme.toString(), lineCount);	
			
		}
			
		//addition
		else  if(inputString.charAt(index) == '+') { 
			lexeme.append(inputString.charAt(index));
			index++;
			token = new Token(TokenType.ADD, lexeme.toString(), lineCount);	
			
		}
		
		//subtraction
		else  if(inputString.charAt(index) == '-') { 
			lexeme.append(inputString.charAt(index));
			index++;
			token = new Token(TokenType.SUBTRACT, lexeme.toString(), lineCount);	
			
		}

		//multiplication
		else  if(inputString.charAt(index) == '*') { 
			lexeme.append(inputString.charAt(index));
			index++;
			token = new Token(TokenType.MULTIPLY, lexeme.toString(), lineCount);	
			
		}
		
		//division
		else if(inputString.charAt(index) == '/') { 
			index++;
			token = new Token(TokenType.DIVIDE, "/", lineCount);
		}
		
		//open parenthesis
		else if(inputString.charAt(index) == '(') { 
			lexeme.append(inputString.charAt(index));
			index++;
			token = new Token(TokenType.OPENPAR, lexeme.toString(), lineCount);	
			
		}	
		
		//close parenthesis
		else if(inputString.charAt(index) == ')') { 
			lexeme.append(inputString.charAt(index));
			index++;
			token = new Token(TokenType.CLOSEPAR, lexeme.toString(), lineCount);	
			
		}	
		
		//open curly parenthesis
		else if(inputString.charAt(index) == '{') { 
			lexeme.append(inputString.charAt(index));
			index++;
			token = new Token(TokenType.OPENCURLYPAR, lexeme.toString(), lineCount);	
			
		}	
		
		//close curly parenthesis
		else if(inputString.charAt(index) == '}') { 
			lexeme.append(inputString.charAt(index));
			index++;
			token = new Token(TokenType.CLOSECURLYPAR, lexeme.toString(), lineCount);	
			
		}	
		
		//open square parenthesis
		else if(inputString.charAt(index) == '[') { 
			lexeme.append(inputString.charAt(index));
			index++;
			token = new Token(TokenType.OPENSQUAREPAR, lexeme.toString(), lineCount);	
			
		}	
		
		//close square parenthesis
		else if(inputString.charAt(index) == ']') { 
			lexeme.append(inputString.charAt(index));
			index++;
			token = new Token(TokenType.CLOSESQUAREPAR, lexeme.toString(), lineCount);	
			
		}
		
		//COMPOUND CHARACTER TOKENS
		//assignment, equals
		else if(inputString.charAt(index) == '=') { 
			lexeme.append(inputString.charAt(index));
			index++;
			if(index < inputString.length() && inputString.charAt(index) == '=') { 
				lexeme.append(inputString.charAt(index));
				index++;
				token = new Token(TokenType.EQUAL, lexeme.toString(), lineCount);
			} else {
				token = new Token(TokenType.ASSIGNMENT, lexeme.toString(), lineCount);
			}
			
		}
		
		//less than, less than or equal, not equal
		else if(inputString.charAt(index) == '<') { 
			lexeme.append(inputString.charAt(index));
			index++;
			if(index < inputString.length() && inputString.charAt(index) == '=' ) { 
				lexeme.append(inputString.charAt(index));
				index++;
				token = new Token(TokenType.LESSTHANOREQ, lexeme.toString(), lineCount);
			} else if (index < inputString.length() && inputString.charAt(index) == '>' ){
				lexeme.append(inputString.charAt(index));
				index++;
				token = new Token(TokenType.NOTEQUAL, lexeme.toString(), lineCount);
			} else {
				token = new Token(TokenType.LESSTHAN, lexeme.toString(), lineCount);
			}
			
		}

		//greater than, greater than or equal
		else if(inputString.charAt(index) == '>') { 
			lexeme.append(inputString.charAt(index));
			index++;
			if(index < inputString.length() && inputString.charAt(index) == '=') { 
				lexeme.append(inputString.charAt(index));
				index++;
				token = new Token(TokenType.GREATTHANOREQ, lexeme.toString(), lineCount);
			} else {
				token = new Token(TokenType.GREATTHAN, lexeme.toString(), lineCount);
			}	
		}
		else {
			lexeme.append(inputString.charAt(index));
			index++;
			token = new Token(TokenType.ERRORUNKOWNCHAR,lexeme.toString(), lineCount);
		}
		
		
		addTokenAndErrorInfo(token);
		return token;
	}
	
	public static TokenType isReservedWord(String lexeme) {
		
		lexeme = lexeme.toLowerCase();
		
		switch(lexeme) { 
			case "and":
				return TokenType.AND;
			case "not":
				return TokenType.NOT;
			case "or":
				return TokenType.OR;
			case "if":
				return TokenType.IF;
			case "then":
				return TokenType.THEN;
			case "else":
				return TokenType.ELSE;
			case "for":
				return TokenType.FOR;
			case "class":
				return TokenType.CLASS;
			case "int":
				return TokenType.INT;
			case "float":
				return TokenType.FLOAT;
			case "get":
				return TokenType.GET;
			case "put":
				return TokenType.PUT;
			case "return":
				return TokenType.RETURN;
			case "program":
				return TokenType.PROGRAM;
			default :
				return TokenType.IDENTIFIER;	
		}
	}
	
	public static void consumeWhitespace() {
		if(index < inputString.length() && Character.isWhitespace(inputString.charAt(index))) {
			while(index < inputString.length() && Character.isWhitespace(inputString.charAt(index))) {
				//line count for error location
				if(inputString.charAt(index) == '\n') { 
					lineCount++;
				}
				index++;
			}	
		}
	}
	
	public static void consumeComment() {
		if(index < inputString.length() && inputString.charAt(index) == '/') { 
			
			//consume line comment
			if(index + 1 < inputString.length() && inputString.charAt(index + 1) == '/' ) {
				index++;
				while(index < inputString.length() && inputString.charAt(index) != '\n') {
					index++;
				}
				lineCount++;
				index++;
				consumeWhitespace();
			//consume block comment
			} else if (index + 1 < inputString.length() && inputString.charAt(index + 1) == '*' ){
				index++;
				index++;
				while(index < inputString.length() &&  index + 1 < inputString.length()
					 && !(inputString.charAt(index) == '*' && inputString.charAt(index + 1) == '/')) {
					index++;
				}
				index++;
				index++;

				consumeWhitespace();				
			} 
		}
	}
	
	public static void resetLexicalAnalyzer() {
		index = 0;
		lineCount = 1;
	}
	
	//adds token sequence and lexical error info on first pass
	public static void addTokenAndErrorInfo(Token token) {
		if(Compiler.firstPass) {
			tokenSequence.add("<Token: " + token.type + ", Lexeme: " + token.lexeme + ", Line: " + token.lineNumber + ">");
			if(token.type == TokenType.ERRORLEADINGZERO) {
				lexicalErrorList.add("Leading Zero Error \nNumber: " + token.lexeme + "\nLine number: " + lineCount);
				lexicalParseSuccess = false;
				lexicalErrorCount++;
			} else if(token.type == TokenType.ERRORTRAILINGZERO) {
				lexicalErrorList.add("Trailing Zero Error \nNumber: " + token.lexeme + "\nLine number: " + lineCount);
				lexicalParseSuccess = false;
				lexicalErrorCount++;				
			} else if (token.type == TokenType.ERRORUNKOWNCHAR) {
				lexicalErrorList.add("Unkown Character Error \nLexeme: " + token.lexeme + "\nLine number: " + lineCount);
				lexicalParseSuccess = false;
				lexicalErrorCount++;
			}
		}
	}
	
	public static void setInputString(String input) {
		inputString = input;
	}
}
