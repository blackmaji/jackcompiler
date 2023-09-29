package br.ufma.ecp.token;
public class Token {

    public final TokenType type;
    public final String lexeme;

    public Token (TokenType type, String lexeme) {
        this.type = type;
        this.lexeme = lexeme;
    }

    public String toString() {
        var type = this.type.toString();
        if (type.equals("NUMBER"))
            type =  "NUMBER";

        if (type.equals("STRING"))
            type =  "STRING";

        if (type.equals("IDENT"))
            type =  "IDENT";

        if (TokenType.isSymbol(lexeme.charAt(0)))
            type = this.type.toString();

        if (TokenType.isKeyword(this.type) )
            type = this.type.toString();
    

        return "<"+ type +">" + lexeme + "</"+ type + ">";
    }
    
}
