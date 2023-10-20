package br.ufma.ecp;

import javax.swing.text.Segment;

import br.ufma.ecp.VMWriter.*;
import br.ufma.ecp.token.Token;
import br.ufma.ecp.token.TokenType;

public class Parser {

   private static class ParseError extends RuntimeException {}

    private Scanner scan;
    private Token currentToken;
    private Token peekToken;
    private StringBuilder xmlOutput = new StringBuilder();
    private VMWriter vmWriter = new VMWriter();
    private int ifLabelNum = 0 ;
    private int whileLabelNum = 0;

    public Parser(byte[] input) {
        scan = new Scanner(input);
        nextToken();
    }

    private void nextToken() {
        currentToken = peekToken;
        peekToken = scan.nextToken();
    }


   public void parse () {
        parseClass();
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
                vmWriter.writePush(VMWriter.Segment.CONST, Integer.parseInt(currentToken.lexeme));
                break;
            case STRING:
                expectPeek(TokenType.STRING);
                var strValue = currentToken.lexeme;
                vmWriter.writePush(VMWriter.Segment.CONST, strValue.length());
                vmWriter.writeCall("String.new", 1);
                for (int i = 0; i < strValue.length(); i++) {
                    vmWriter.writePush(VMWriter.Segment.CONST, strValue.charAt(i));
                    vmWriter.writeCall("String.appendChar", 2);
                }
                break;
            case FALSE:
            case NULL:
            case TRUE:
                expectPeek(TokenType.FALSE, TokenType.NULL, TokenType.TRUE);
                vmWriter.writePush(VMWriter.Segment.CONST, 0);
                if (currentToken.type == TokenType.TRUE)
                    vmWriter.writeArithmetic(Command.NOT);
                break;
            case THIS:
                expectPeek(TokenType.THIS);
                vmWriter.writePush(VMWriter.Segment.POINTER, 0);
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

            case MINUS:
            case NOT:
                expectPeek(TokenType.MINUS, TokenType.NOT);
                var op = currentToken.type;
                parseTerm();
                if (op == TokenType.MINUS)
                    vmWriter.writeArithmetic(Command.NEG);
                else
                    vmWriter.writeArithmetic(Command.NOT);
        
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
            case WHILE:
                parseWhile();
                break;
            case IF:
                parseIf();
                break;
            case RETURN:
                parseReturn();
                break;
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
        return op != "" && "+-*/<>=~&|".contains(op);
   }
    public void compileOperators(TokenType type) {

        if (type == TokenType.ASTERISK) {
            vmWriter.writeCall("Math.multiply", 2);
        } else if (type == TokenType.SLASH) {
            vmWriter.writeCall("Math.divide", 2);
        } else {
            vmWriter.writeArithmetic(typeOperator(type));
        }
    }

    private Command typeOperator(TokenType type) {
        if (type == TokenType.PLUS)
            return Command.ADD;
        if (type == TokenType.MINUS)
            return Command.SUB;
        if (type == TokenType.LT)
            return Command.LT;
        if (type == TokenType.GT)
            return Command.GT;
        if (type == TokenType.EQ)
            return Command.EQ;
        if (type == TokenType.AND)
            return Command.AND;
        if (type == TokenType.OR)
            return Command.OR;
        return null;
    }


    void parseExpression() {
        printNonTerminal("expression");
        parseTerm();
        while (isOperator(peekToken.lexeme)) {
            var ope = peekToken.type;
            expectPeek(peekToken.type);
            parseTerm();
            compileOperators(ope);
        }
        printNonTerminal("/expression");
    }

    void parseParameterList() {
        printNonTerminal("parameterList");

        if (!peekTokenIs(TokenType.RPAREN))
        {
            expectPeek(TokenType.INT, TokenType.CHAR, TokenType.BOOLEAN, TokenType.IDENT);

            expectPeek(TokenType.IDENT);

            while (peekTokenIs(TokenType.COMMA)) {
                expectPeek(TokenType.COMMA);
                expectPeek(TokenType.INT, TokenType.CHAR, TokenType.BOOLEAN, TokenType.IDENT);

                expectPeek(TokenType.IDENT);
            }

        }
        printNonTerminal("/parameterList");
    }

    void parseSubroutineBody() {

        printNonTerminal("subroutineBody");
        expectPeek(TokenType.LBRACE);
        while (peekTokenIs(TokenType.VAR)) {
            parseVarDec();
        }
        parseStatements();
        expectPeek(TokenType.RBRACE);
        printNonTerminal("/subroutineBody");
    }

    void parseReturn() {
        printNonTerminal("returnStatement");
        expectPeek(TokenType.RETURN);
        if (!peekTokenIs(TokenType.SEMICOLON)) {
            parseExpression();
        } else {
            vmWriter.writePush(VMWriter.Segment.CONST,0);
        }

        expectPeek(TokenType.SEMICOLON);
        vmWriter.writeReturn();

        printNonTerminal("/returnStatement");
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
    
        var labelTrue = "IF_TRUE" + ifLabelNum;
        var labelFalse = "IF_FALSE" + ifLabelNum;
        var labelEnd = "IF_END" + ifLabelNum;
    
        ifLabelNum++;
        
        expectPeek(TokenType.IF);
        expectPeek(TokenType.LPAREN);
        parseExpression();
        expectPeek(TokenType.RPAREN);
    
        vmWriter.writeIf(labelTrue);
        vmWriter.writeGoto(labelFalse);
        vmWriter.writeLabel(labelTrue);
        
        expectPeek(TokenType.LBRACE);
        parseStatements();
        expectPeek(TokenType.RBRACE);
        if (peekTokenIs(TokenType.ELSE)){
            vmWriter.writeGoto(labelEnd);
        }
    
        vmWriter.writeLabel(labelFalse);
    
        if (peekTokenIs(TokenType.ELSE)){
            expectPeek(TokenType.ELSE);
            expectPeek(TokenType.LBRACE);
            parseStatements();
            expectPeek(TokenType.RBRACE);
            vmWriter.writeLabel(labelEnd);
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

    void parseWhile() {
        printNonTerminal("whileStatement");

        var labelTrue = "WHILE_EXP" + whileLabelNum;
        var labelFalse = "WHILE_END" + whileLabelNum;
        whileLabelNum++;

        vmWriter.writeLabel(labelTrue);

        expectPeek(TokenType.WHILE);
        expectPeek(TokenType.LPAREN);
        parseExpression();

        vmWriter.writeArithmetic(Command.NOT);
        vmWriter.writeIf(labelFalse);

        expectPeek(TokenType.RPAREN);
        expectPeek(TokenType.LBRACE);
        parseStatements();

        vmWriter.writeGoto(labelTrue);
        vmWriter.writeLabel(labelFalse); 

        expectPeek(TokenType.RBRACE);
        printNonTerminal("/whileStatement");
    }

    void parseVarDec() {
        printNonTerminal("varDec");
        expectPeek(TokenType.VAR);

        expectPeek(TokenType.INT, TokenType.CHAR, TokenType.BOOLEAN, TokenType.IDENT);


        expectPeek(TokenType.IDENT);

        while (peekTokenIs(TokenType.COMMA)) {
            expectPeek(TokenType.COMMA);
            expectPeek(TokenType.IDENT);
        }

        expectPeek(TokenType.SEMICOLON);
        printNonTerminal("/varDec");
    }


    void parseClassVarDec() {
        printNonTerminal("classVarDec");
        expectPeek(TokenType.FIELD, TokenType.STATIC);

        expectPeek(TokenType.IDENT, TokenType.INT, TokenType.CHAR, TokenType.BOOLEAN);
        expectPeek(TokenType.IDENT);

        while (peekTokenIs(TokenType.COMMA)) {
            expectPeek(TokenType.COMMA);
            expectPeek(TokenType.IDENT);
        }

        expectPeek(TokenType.SEMICOLON);
        printNonTerminal("/classVarDec");
    }

    void parseSubroutineDec() {
        printNonTerminal("subroutineDec");

        ifLabelNum = 0;
        whileLabelNum = 0;
        
        expectPeek(TokenType.CONSTRUCTOR, TokenType.FUNCTION, TokenType.METHOD);

        expectPeek(TokenType.VOID, TokenType.INT, TokenType.CHAR, TokenType.BOOLEAN, TokenType.IDENT);
        expectPeek(TokenType.IDENT);

        expectPeek(TokenType.LPAREN);
        parseParameterList();
        expectPeek(TokenType.RPAREN);
        parseSubroutineBody();

        printNonTerminal("/subroutineDec");
    }

    void parseClass() {
        printNonTerminal("class");
        expectPeek(TokenType.CLASS);
        expectPeek(TokenType.IDENT);

        expectPeek(TokenType.LBRACE);

        while (peekTokenIs(TokenType.STATIC) || peekTokenIs(TokenType.FIELD)) {
            parseClassVarDec();
        }

        while (peekTokenIs(TokenType.FUNCTION) || peekTokenIs(TokenType.CONSTRUCTOR) || peekTokenIs(TokenType.METHOD)) {
            parseSubroutineDec();
        }

        expectPeek(TokenType.RBRACE);

        printNonTerminal("/class");
    }

    public String VMOutput() {
        return vmWriter.vmOutput();
    }
}