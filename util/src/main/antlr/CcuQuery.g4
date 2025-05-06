grammar CcuQuery;

filter
    : logical_or
    ;

logical_or
    : logical_and (OR logical_and)*
    ;

logical_and
    : term (AND term)*
    ;

term
    : paren_term
    | missing_term
    | comparison_term
    | dereference_term
    ;

paren_term
    : LEFT_PAREN filter RIGHT_PAREN
    ;

missing_term
    : NOT IDENTIFIER
    ;

comparison_term
    : IDENTIFIER (comparison_operator scalar)?
    ;

dereference_term
    : IDENTIFIER DEREFERENCE term
    ;

comparison_operator
    : equal
    | not_equal
    | greater_than
    | less_than
    | less_than_equal
    | greater_than_equal
    ;

equal
    : EQUAL
    ;

not_equal
    : NOT_EQUAL
    ;

greater_than
    : GREATER_THAN
    ;

less_than
    : LESS_THAN
    ;

less_than_equal
    : LESS_THAN_EQUAL
    ;

greater_than_equal
    : GREATER_THAN_EQUAL
    ;

scalar
    : null_scalar
    | marker_scalar
    | bool_scalar
    | ref_scalar
    | str_scalar
    | uri_scalar
    | number_scalar
    | date_scalar
    | time_scalar
    | datetime_scalar
    | coord_scalar
    | xstr_scalar
    ;

null_scalar
    : NULL_SCALAR
    ;

marker_scalar
    : MARKER_SCALAR
    ;

bool_scalar
    : BOOL_SCALAR
    ;

ref_scalar
    : REF_SCALAR
    ;

str_scalar
    : STR_SCALAR
    ;

uri_scalar
    : URI_SCALAR
    ;

number_scalar
    : NUMBER_SCALAR
    ;

date_scalar
    : DATE_SCALAR
    ;

time_scalar
    : TIME_SCALAR
    ;

datetime_scalar
    : DATETIME_SCALAR
    ;

coord_scalar
    : COORD_SCALAR
    ;

xstr_scalar
    : XSTR_SCALAR
    ;

AND
    : 'and'
    ;

OR
    : 'or'
    ;

NOT
    : 'not'
    ;

LEFT_PAREN
    : '('
    ;

RIGHT_PAREN
    : ')'
    ;

EQUAL
    : '=='
    ;

NOT_EQUAL
    : '!='
    ;

LESS_THAN
    : '<'
    ;

LESS_THAN_EQUAL
    : '<='
    ;

GREATER_THAN
    : '>'
    ;

GREATER_THAN_EQUAL
    : '>='
    ;

DEREFERENCE
    : '->'
    ;

BOOL_SCALAR
    : 'true'
    | 'false'
    ;

IDENTIFIER
    : ALPHA_LOWER IDENTIFIER_CHARS*
    ;

fragment IDENTIFIER_CHARS
    : ALPHANUMERIC
    | '_'
    ;

NULL_SCALAR
    : 'N'
    ;

MARKER_SCALAR
    : 'M'
    ;

REF_SCALAR
    : '@' REF_CHAR+ (' "' STR_CHAR* '"')?
    ;

fragment REF_CHAR
    : ALPHANUMERIC
    | '_'
    | '-'
    | '.'
    | '~'
    ;

STR_SCALAR
    : '"' STR_CHAR* '"'
    ;

fragment STR_CHAR
    : ESC
    | ~'"'
    ;

URI_SCALAR
    : '`' URI_CHAR* '`'
    ;

fragment URI_CHAR
    : ESC
    | ~'`'
    ;

DATE_SCALAR
    : YEAR '-' MONTH'-' DAY
    ;

TIME_SCALAR
    : HOUR ':' MINUTE ':' SECOND FRACTIONAL_SECONDS?
    ;

DATETIME_SCALAR
    : YEAR '-' MONTH '-' DAY 'T' HOUR ':' MINUTE ':' SECOND FRACTIONAL_SECONDS? ZONE_OFFSET
    ;

fragment YEAR
    : DIGIT DIGIT DIGIT DIGIT
    ;

fragment MONTH
    : DIGIT DIGIT
    ;

fragment DAY
    : DIGIT DIGIT
    ;

fragment HOUR
    : DIGIT DIGIT
    ;

fragment MINUTE
    : DIGIT DIGIT
    ;

fragment SECOND
    : DIGIT DIGIT
    ;

fragment FRACTIONAL_SECONDS
    : '.' (DIGIT)+
    ;


fragment ZONE_OFFSET
    : 'Z'
    | 'Z UTC'
    | OFFSET ' ' TIMEZONE_CHAR+
    ;

fragment OFFSET
    : PLUS_OR_MINUS DIGIT DIGIT (':'? DIGIT DIGIT)?
    ;

fragment PLUS_OR_MINUS
    : ('+' | '-')
    ;

fragment TIMEZONE_CHAR
    : ALPHA
    | '_'
    ;

COORD_SCALAR
    : 'C(' DECIMAL ',' DECIMAL ')'
    ;

NUMBER_SCALAR
    : DECIMAL UNIT_CHAR*
    ;

fragment UNIT_CHAR
    : ALPHA
    | '%'
    | '_'
    | '/'
    | '$'
    | [\u0081-\uFFFE]
    ;

XSTR_SCALAR
    : XSTR_TYPE '("' STR_CHAR* '")'
    ;

fragment XSTR_TYPE
    : ALPHA_UPPER XSTR_TYPE_CHAR*
    ;

fragment XSTR_TYPE_CHAR
    : ALPHANUMERIC
    | '_'
    ;

WS
   : [ \r\n\t]+ -> channel(HIDDEN)
   ;

fragment DECIMAL
    : '-'? DIGIT+ ('.' DIGIT+)?
    ;

fragment ALPHANUMERIC
    : ALPHA
    | DIGIT
    ;

fragment ALPHA
    : ALPHA_UPPER
    | ALPHA_LOWER
    ;

fragment ALPHA_UPPER
    : [A-Z]
    ;

fragment ALPHA_LOWER
    : [a-z]
    ;

fragment DIGIT
    : [0-9]
    ;

fragment ESC
    : '\\' (["`] | UNICODE)
    ;

fragment UNICODE
   : 'u' HEX HEX HEX HEX
   ;

fragment HEX
   : [0-9a-fA-F]
   ;