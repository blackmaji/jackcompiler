package br.ufma.ecp;

import javax.swing.text.Segment;

import br.ufma.ecp.token.Token;
import br.ufma.ecp.token.TokenType;

public class Parser {

   private static class ParseError extends RuntimeException {}

    private Scanner scan;
    private Token currentToken;
    private Token peekToken;
    private StringBuilder xmlOutput = new StringBuilder();

    public Parser(byte[] input) {
        scan = new Scanner(input);
        nextToken();
    }

    private void nextToken() {
        currentToken = peekToken;
        peekToken = scan.nextToken();
    }


   public void parse () {
    }

    // funções auxiliares
    public String XMLOutput() {
        return xmlOutput.toString();
    }

    private void printNonTerminal(String nterminal) {
        xmlOutput.append(String.format("<%s>\r\n", nterminal));
    }


    boolean peekTokenIs(TokenType type) {
        return peekToken.type == type;
    }

    boolean currentTokenIs(TokenType type) {
        return currentToken.type == type;
    }

    private void expectPeek(TokenType... types) {
        for (TokenType type : types) {
            if (peekToken.type == type) {
                expectPeek(type);
                return;
            }
        }

       throw error(peekToken, "Expected a statement");

    }

    private void expectPeek(TokenType type) {
        if (peekToken.type == type) {
            nextToken();
            xmlOutput.append(String.format("%s\r\n", currentToken.toString()));
        } else {
            throw error(peekToken, "Expected "+type.name());
        }
    }


    private static void report(int line, String where,
        String message) {
            System.err.println(
            "[line " + line + "] Error" + where + ": " + message);
    }


    private ParseError error(Token token, String message) {
        if (token.type == TokenType.EOF) {
            report(token.line, " at end", message);
        } else {
            report(token.line, " at '" + token.lexeme + "'", message);
        }
        return new ParseError();
    }

    void parseTerm() {
        printNonTerminal("term");
        switch (peekToken.type) {
            case NUMBER:
                expectPeek(TokenType.NUMBER);
                break;
            case STRING:
                expectPeek(TokenType.STRING);
                break;
            case FALSE:
            case NULL:
            case TRUE:
                expectPeek(TokenType.FALSE, TokenType.NULL, TokenType.TRUE);
                break;
            case THIS:
                expectPeek(TokenType.THIS);
                break;
            case IDENT:
                expectPeek(TokenType.IDENT);

                if (peekTokenIs(TokenType.LPAREN) || peekTokenIs(TokenType.DOT)) {
                    parseSubroutineCall();
                } else { 
                    if (peekTokenIs(TokenType.LBRACKET)) {
                        expectPeek(TokenType.LBRACKET);
                        parseExpression();

                        expectPeek(TokenType.RBRACKET);
                    }
                }
                break;

            default:
                throw error(peekToken, "term expected");
        }
    
        printNonTerminal("/term");
    }

    void parseStatements() {
        printNonTerminal("statements");
        while (peekToken.type == TokenType.WHILE ||
                peekToken.type == TokenType.IF ||
                peekToken.type == TokenType.LET ||
                peekToken.type == TokenType.DO ||
                peekToken.type == TokenType.RETURN) {
            parseStatement();
        }

        printNonTerminal("/statements");
    }

    int parseExpressionList() {
        printNonTerminal("expressionList");

        var nArgs = 0;

        if (!peekTokenIs(TokenType.RPAREN))
        {
            parseExpression();
            nArgs = 1;
        }

        while (peekTokenIs(TokenType.COMMA)) {
            expectPeek(TokenType.COMMA);
            parseExpression();
            nArgs++;
        }

        printNonTerminal("/expressionList");
        return nArgs;
    }


    void parseStatement() {
        switch (peekToken.type) {
            case LET:
                parseLet();
                break;
            /*case WHILE:
                parseWhile();
                break;*/
            case IF:
                parseIf();
                break;
            /*case RETURN:
                parseReturn();
                break;*/
            case DO:
                parseDo();
                break;
            default:
                throw error(peekToken, "Expected a statement");
        }
    }

    void parseSubroutineCall() {
        int nArgs = 0;
    
        if (peekTokenIs(TokenType.LPAREN)) {
            expectPeek(TokenType.LPAREN);
            nArgs = parseExpressionList() + 1;
            expectPeek(TokenType.RPAREN);
        } else {
            expectPeek(TokenType.DOT);
            expectPeek(TokenType.IDENT);
            expectPeek(TokenType.LPAREN);
            nArgs += parseExpressionList();
            expectPeek(TokenType.RPAREN);
        }
    }
    


    static public boolean isOperator(String op) {
        return "+-*/<>=~&|".contains(op);
   }


    void parseExpression() {
        printNonTerminal("expression");
        parseTerm ();
        while (isOperator(peekToken.lexeme)) {
            expectPeek(peekToken.type);
            parseTerm();
        }
        printNonTerminal("/expression");
    }

    void parseLet() {

        printNonTerminal("letStatement");
        expectPeek(TokenType.LET);
        expectPeek(TokenType.IDENT);

        if (peekTokenIs(TokenType.LBRACKET)) {
            expectPeek(TokenType.LBRACKET);
            parseExpression();
            expectPeek(TokenType.RBRACKET);

        }

        expectPeek(TokenType.EQ);
        parseExpression();
        expectPeek(TokenType.SEMICOLON);

        printNonTerminal("/letStatement");
    }

    void parseIf() {
        printNonTerminal("ifStatement");
    
        expectPeek(TokenType.IF);
        expectPeek(TokenType.LPAREN);
        parseExpression();
        expectPeek(TokenType.RPAREN);
    
        expectPeek(TokenType.LBRACE);
        parseStatements();
        expectPeek(TokenType.RBRACE);

        if (peekTokenIs(TokenType.ELSE))
        {
            expectPeek(TokenType.ELSE);

            expectPeek(TokenType.LBRACE);

            parseStatements();

            expectPeek(TokenType.RBRACE);
        }

        printNonTerminal("/ifStatement");
    }

    void parseDo() {
        printNonTerminal("doStatement");

        expectPeek(TokenType.DO);
        expectPeek(TokenType.IDENT);
        parseSubroutineCall();
        expectPeek(TokenType.SEMICOLON);

        printNonTerminal("/doStatement");
    }


}
