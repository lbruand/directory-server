header {
/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *  
 *    http://www.apache.org/licenses/LICENSE-2.0
 *  
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License. 
 *  
 */
package org.apache.directory.shared.ldap.schema.syntax;

import java.util.List;
import java.util.ArrayList;

}


/**
 * An antlr generated schema lexer. This is a sub-lexer.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
class AntlrSchemaValueLexer extends Lexer;

options    {
    k = 3 ;
    exportVocab=AntlrSchemaValue ;
    charVocabulary = '\3'..'\377' ;
    caseSensitive = true ;
    defaultErrorHandler = false ;
}

//WHSP : (' ') {$setType(Token.SKIP);} ;
//SP : ( ' ' )+ { setText(" "); };
SP
    :
    ( options {greedy=true;} :
    ' '
    |
    '\t'
    |
    '\r' (options {greedy=true;} : '\n')? { newline(); } 
    |
    '\n' { newline(); }
    )+
    { setText(" "); }
    ;

LPAR : '(' ;
RPAR : ')' ;
protected LDIGIT : '1'..'9' ;
protected DIGIT : '0'..'9' ; 
protected NUMBER : DIGIT | ( LDIGIT (DIGIT)+ ) ;
protected NUMERICOID : NUMBER ( '.' NUMBER )+ ;

QUOTE : '\'' ;
DOLLAR : '$' ;
LCURLY : '{' ;
RCURLY : '}' ;
DESCR : ( 'a'..'z' | 'A'..'Z' ) ( 'a'..'z' | 'A'..'Z' | '0'..'9' | '-' )* ;
LEN : LCURLY n:NUMBER RCURLY { setText(n.getText()); } ;

NUMBER_OR_NUMERICOID :
    ( NUMBER '.' ) => NUMERICOID { $setType( NUMERICOID ); }
    |
    ( NUMBER ) { $setType( NUMBER ); }
    ;

/**
 * An antlr generated schema parser. This is a sub-parser used to parse
 * numericoid, oid, oids, qdescr, qdescrs according to RFC4512.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
class AntlrSchemaValueParser extends Parser;
options    {
    k = 3 ;
    defaultErrorHandler = false ;
    //buildAST=true ;
}


    /**
     * noidlen = numericoid [ LCURLY len RCURLY ]
     * len = number
     */
noidlen returns [AntlrSchemaParser.NoidLen noidlen = new AntlrSchemaParser.NoidLen()]
    :
    ( 
      o:NUMERICOID { noidlen.noid = o.getText(); } 
      (
        l:LEN { noidlen.len = Integer.parseInt(l.getText()); } 
      )?
    )
    ;


    /**
     * numericoid = number 1*( DOT number )
     */
numericoid returns [String numericoid=null]
    : 
    (
        (SP)? n:NUMERICOID (SP)? { numericoid = n.getText(); } 
    )
    ;


    /**
     * oid = descr / numericoid
     * numericoid = number 1*( DOT number )
     * descr = keystring
     * keystring = leadkeychar *keychar
     * leadkeychar = ALPHA
     * keychar = ALPHA / DIGIT / HYPHEN
     * number  = DIGIT / ( LDIGIT 1*DIGIT )
     *
     */
oid returns [String oid=null]
    : 
    (
        (SP)? 
        (
            n:NUMERICOID { oid = n.getText(); }
        | 
            d:DESCR { oid = d.getText(); }
        )
        (SP)?
    )
    ;


    /**
     * oids = oid / ( LPAREN WSP oidlist WSP RPAREN )
     * oidlist = oid *( WSP DOLLAR WSP oid )
     */
oids returns [List<String> oids]
    {
        oids = new ArrayList<String>();
        String oid = null;
    }
    :
    (
        ( 
            oid=oid { oids.add(oid); } 
        )
    |
        ( 
            LPAR 
            oid=oid { oids.add(oid); } 
            ( 
                DOLLAR 
                oid=oid { oids.add(oid); } 
            )* 
            RPAR 
        )
    )
    ;


    /**
     * qdescr = SQUOTE descr SQUOTE
     */
qdescr returns [String qdescr=null]
    : 
    ( 
        (SP)?
        QUOTE 
        d:DESCR { qdescr = d.getText(); } 
        QUOTE
    )
    ; 


    /**
     * qdescrs = qdescr / ( LPAREN WSP qdescrlist WSP RPAREN )
     * qdescrlist = [ qdescr *( SP qdescr ) ]
     */
qdescrs returns [List<String> qdescrs]
    {
        qdescrs = new ArrayList<String>();
        String qdescr = null;
    }
    :
    (
        ( 
            qdescr=qdescr { qdescrs.add(qdescr); } 
        )
    |
        ( 
            LPAR 
            qdescr=qdescr { qdescrs.add(qdescr); } 
            (
                SP
                qdescr=qdescr { qdescrs.add(qdescr); } 
            )* 
            (SP)?
            RPAR 
        )
    )
    ;
    
    /**
     * ruleid = number
     * number  = DIGIT / ( LDIGIT 1*DIGIT )
     *
     */
ruleid returns [Integer ruleid=null]
    : 
    (
        (SP)? 
        n:NUMBER { ruleid = Integer.parseInt(n.getText()); }
    )
    ;


    /**
     * ruleids = ruleid / ( LPAREN WSP ruleidlist WSP RPAREN )
     * ruleidlist = ruleid *( SP ruleid )
     */
ruleids returns [List<Integer> ruleids]
    {
        ruleids = new ArrayList<Integer>();
        Integer ruleid = null;
    }
    :
    (
        ( 
            ruleid=ruleid { ruleids.add(ruleid); } 
        )
    |
        ( 
            LPAR 
            ruleid=ruleid { ruleids.add(ruleid); } 
            ( 
                SP
                ruleid=ruleid { ruleids.add(ruleid); } 
            )* 
            (SP)?
            RPAR 
        )
    )
    ;
    