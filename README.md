# Clojure dev utils

My personal clojure utilities to work with clojure CLI.

Extracted from [my dotfiles](https://github.com/jlesquembre/dotfiles), used with
my personal
[deps.edn](https://github.com/jlesquembre/dotfiles/blob/master/dotfiles/clojure/deps.edn).
Take a look to the `:user` alias.

## Installation

Add the dependency:

```edn

{:aliases
 {:user
  {:extra-deps {me.lafuente/clj-dev-utils {:git/url "https://github.com/jlesquembre/clj-dev-utils"
                                           :tag     "1.0"}}}}}
```

Alternatively, if you prefer to clone the project locally:

```edn
{:aliases
 {:user
  {:extra-deps {me.lafuente/clj-dev-utils {:local/root "/path/to/clj-dev-utils"}}}}}
```

## Usage

```bash
clj -X:user
```

That command will execute the `local-utils/init` function. It takes 4 optional
argument:

- `main`: Main entry point for your program. It will require that namespace.
- `exec`: Boolean, defaults to _false_. If true, the main function will be
  executed.
- `args`: Arguments to the main function.
- `nrepl`: Boolean, defaults to _true_. If true, an nrepl server will be
  started.

E.g.:

```bash
clj -X:user :main myname.myapp/-main :exec true :args '["foo"]' :nrepl true
```

## REPL helpers

A namespace called `.` with dev utilities is created. We use
[dot-slash-2](https://github.com/gfredericks/dot-slash-2) for that. Once your
editor is connected to the REPL, type `./` to see the helper.
