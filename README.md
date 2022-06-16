# Clojure dev utils

My personal clojure utilities to work with clojure CLI.

Extracted from [my dotfiles](https://github.com/jlesquembre/dotfiles), used with
my personal
[deps.edn](https://github.com/jlesquembre/dotfiles/blob/master/dotfiles/clojure/deps.edn).
Take a look to the `:user` alias.

## Installation

Add the dependency to your project, or to your `~/.config/clojure/deps.edn`:

```edn
{:aliases
 {:user
  {:extra-deps {me.lafuente/clj-dev-utils {:git/url "https://github.com/jlesquembre/clj-dev-utils"
                                           :git/tag "1.0"
                                           :git/sha "7d76d43"}}
   :exec-fn local-utils/init}}}

```

Alternatively, if you prefer to clone the project locally:

```edn
{:aliases
 {:user
  {:extra-deps {me.lafuente/clj-dev-utils {:local/root "/path/to/clj-dev-utils"}}
   :exec-fn local-utils/init}}}
```

## Usage

```bash
clj -X:user
```

That command will execute the `local-utils/init` function. It takes some
optional arguments:

- `main`: Main entry point for your program. It will require that namespace.
- `exec`: Boolean, defaults to _false_. If true, the main function will be
  executed.
- `args`: Arguments to the main function.
- `nrepl`: Boolean, defaults to _true_. If true, an nrepl server will be
  started.
- `portal`: Boolean, defaults to _false_. If true, calls `(portal.api/open)`
- `extra-requires`: Comma separated lists of libs to require. Executes
  `(require '[lib])` for each lib.

E.g.:

```bash
clj -X:user :main myname.myapp/-main :exec true :args '["foo"]' :nrepl true :portal true :extra-requires dev,extra-lib
```

## REPL helpers

A namespace called `.` with dev utilities is created. We use
[dot-slash-2](https://github.com/gfredericks/dot-slash-2) for that. Once your
editor is connected to the REPL, type `./` to see the helper.
