
public class Token {
	
	public TokenType type;
	public String lexeme;
	public int lineNumber;
	
	public Token(TokenType type, String lexeme, int lineNumber) {
		this.type = type;
		this.lexeme = lexeme;
		this.lineNumber = lineNumber;
	}

	public Token() {}
}
