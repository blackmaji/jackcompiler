package br.ufma.ecp.token;
public class Token {

    public final TokenType type;
    public final String lexeme;
    final int line;

    public Token (TokenType type, String lexeme, int line) {
        this.type = type;
        this.lexeme = lexeme;
        this.line = line;
    }

    public String toString() {
        var type = this.type.toString();
        if (type.equals("NUMBER"))
            type =  "integerConstant";

        if (type.equals("STRING"))
            type =  "stringConstant";

        if (type.equals("IDENT"))
            type =  "identifier";

        if (TokenType.isSymbol(lexeme.charAt(0)))
            type = this.type.toString();

        if (TokenType.isKeyword(this.type) )
            type = this.type.toString();
    

        return "<"+ type +">" + lexeme + "</"+ type + ">";
    }
    
}
