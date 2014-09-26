/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package datalogger.server.parser;


import datalogger.server.parser.CalculatorParser3.CalcNode;
import org.parboiled.Rule;
import org.parboiled.annotations.BuildParseTree;
import org.parboiled.support.Var;

/**
 * A calculator parser defining the same language as the CalculatorParser3 but using a rule building helper methods
 * to Factor out common constructs.
 */
@BuildParseTree
public class CalculatorParser4 extends CalculatorParser<CalcNode> {

    @Override
    public Rule InputLine() {
        return sequence(Expression(), EOI);
    }

    public Rule Expression() {
        return OperatorRule(Term(), firstOf("+ ", "- "));
    }

    public Rule Term() {
        return OperatorRule(Factor(), firstOf("* ", "/ "));
    }

    public Rule Factor() {
        // by using toRule("^ ") instead of Ch('^') we make use of the fromCharLiteral(...) transformation below
        return OperatorRule(Atom(), toRule("^ "));
    }

    public Rule OperatorRule(Rule subRule, Rule operatorRule) {
        Var<String> op = new Var<String>();
        return sequence(
                subRule,
                zeroOrMore(
                        operatorRule, op.set(""+matchedChar()),
                        subRule,
                        push(new CalcNode(op.get(), pop(1), pop()))
                )
        );
    }

    public Rule Atom() {
        return firstOf(Number(), Function(), Parens());
    }

    public Rule Function() {
        return firstOf(SquareRoot(), Fun());
    }

    public Rule SquareRoot() {
        return sequence("SQRT", Parens(), push(new CalcNode("R", pop(), null)));
    }

    public Rule Fun() {
        return sequence("FUN", Parens(), push(new CalcNode("fun", pop(), null)));
    }

    public Rule Parens() {
        return sequence("( ", Expression(), ") ");
    }

    public Rule Number() {
        return sequence(
                sequence(
                        optional(ch('-')),
                        oneOrMore(Digit()),
                        optional(ch('.'), oneOrMore(Digit()))
                ),
                // the action uses a default string in case it is run during error recovery (resynchronization)
                push(new CalcNode(Double.parseDouble(matchOrDefault("0")))),
                WhiteSpace()
        );
    }

    public Rule Digit() {
        return charRange('0', '9');
    }

    public Rule WhiteSpace() {
        return zeroOrMore(anyOf(" \t\f"));
    }

    // we redefine the rule creation for string literals to automatically match trailing whitespace if the string
    // literal ends with a space character, this way we don't have to insert extra whitespace() rules after each
    // character or string literal
    @Override
    protected Rule fromStringLiteral(String string) {
        return string.endsWith(" ") ?
                sequence(string(string.substring(0, string.length() - 1)), WhiteSpace()) :
                string(string);
    }

    //**************** MAIN ****************

    public static void main(String[] args) {
        main(CalculatorParser4.class);
    }
}