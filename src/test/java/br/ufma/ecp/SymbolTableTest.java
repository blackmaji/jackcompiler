package br.ufma.ecp;

import static org.junit.Assert.assertEquals;

import java.nio.charset.StandardCharsets;

import org.junit.Test;

public class SymbolTableTest {
    @Test
    public void testSimpleFunctions () {
        var input = """
            class Main {
 
                function int soma (int x, int y) {
                        return  30;
                 }
                
                 function void main () {
                        var int d;
                        return;
                  }
                
                }
            """;;
        var parser = new Parser(input.getBytes(StandardCharsets.UTF_8));
        parser.parse();
        String actual = parser.VMOutput();
        String expected = """
            function Main.soma 0
            push constant 30
            return
            function Main.main 1
            push constant 0
            return    
                """;
        assertEquals(expected, actual);
    }
}
