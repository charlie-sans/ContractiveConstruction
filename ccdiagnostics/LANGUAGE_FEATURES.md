# Construct Language Features

This document describes the key features of the Construct programming language as implemented in this project.

## Top-Level Statements

All statements must start with one of:

- `let` — variable/constant binding
  ```construct
  let x = 42
  let name = "Alice"
  ```
- `fn` — function definition
  ```construct
  fn add(a, b) = a + b
  ```
- `type` — type alias definition
  ```construct
  type Point = {x: Int, y: Int}
  ```
- `import` — module import
  ```construct
  import math
  ```

## Control Flow

- ✅ `if` / `then` / `else` expressions
  ```construct
  let result = if x > 0 then "positive" else "non-positive"
  ```
- ✅ `match` expressions for pattern matching
  ```construct
  let kind = match x:
    | 0 -> "zero"
    | 1 -> "one"
    | _ -> "other"
  ```
- ✅ `for` loops - iterate over lists
  ```construct
  for i in [1, 2, 3] do
    dump(i)
  end
  ```
- ✅ `while` loops - conditional iteration
  ```construct
  while x < 10 do
    dump(x)
  end
  ```

## Functions

- User-defined functions via `fn`
  ```construct
  fn square(n) = n * n
  ```
- Function application: `foo(x, y)`
  ```construct
  let y = add(2, 3)
  ```
- First-class functions (can be passed as values)
  ```construct
  let f = square
  let result = f(5)
  ```

## Types

- Primitive types: `Int`, `Float`, `Bool`, `String`
  ```construct
  let a: Int = 10
  let b: Float = 3.14
  let c: Bool = true
  let d: String = "hello"
  ```
- Composite types: Lists `[a]`, Tuples `(a, b, c)`, Records `{x: Int, y: Int}`
  ```construct
  let nums = [1, 2, 3]
  let pair = (1, "one")
  let pt = {x: 1, y: 2}
  ```
- Type aliases via `type`
  ```construct
  type Name = String
  ```
- (Planned) Algebraic data types, enums, generics

## Pattern Matching

- `match` expressions for destructuring and control flow
  ```construct
  let desc = match value:
    | true -> "yes"
    | false -> "no"
  ```
- Wildcards (`_`) and literal patterns
  ```construct
  let kind = match x:
    | 0 -> "zero"
    | _ -> "other"
  ```
- (Planned) More advanced pattern matching

## Modules & Imports

- Import other modules/files via `import`
  ```construct
  import math
  ```
- (Planned) Namespaces, module system

## Built-in Functions

- Standard library functions:
  ```construct
  dump(x)
  print("Hello")
  nl()
  let n = len([1,2,3])
  let s = concat("a", "b")
  let eqTest = eq(1, 1)
  let sub = substr("hello", 1, 3)
  let idx = indexOf("hello", "e")
  let str = toStr(42)
  let i = toInt("42")
  let f = toFloat("3.14")
  let b = toBool("true")
  let absVal = abs(-5)
  let sq = sqrt(9)
  let p = pow(2, 8)
  let mx = max(1, 2)
  let mn = min(1, 2)
  ```

## Comments

- Single-line comments:
  ```construct
  # This is a comment
  let x = 5 # Inline comment
  ```
- Multi-line comments:
  ```construct
  /*
    This is a
    multi-line comment
  */
  let y = 10
  ```

## Error Handling

- Parser provides detailed error messages with line, column, token type, and value
  ```
  Parse error: Top-level statement must start with 'let', 'fn', 'type', or 'import' at line 1, column 1. Got 'IDENTIFIER' with value 'foo'
  ```
- (Planned) Language-level error handling (try/catch, option types)

## Other Planned Features

- Type inference
- Operator overloading
- Documentation comments
- Collection and string utilities
- Custom operators

---

*This document will be updated as new features are added to the language.*
