#!/bin/bash -e

# Runs every non-empty, non-comment line of the given file as a shell command.
# Stops on the first failing command and fails if the file has no commands.

COMMANDS_FILE=$1

if [ ! -f "$COMMANDS_FILE" ]; then
  echo "Commands file not found: $COMMANDS_FILE"
  exit 1
fi

COUNT=0
while IFS= read -r CMD || [ -n "$CMD" ]; do
  CMD="${CMD#"${CMD%%[![:space:]]*}"}"
  if [ -z "$CMD" ] || [[ "$CMD" == \#* ]]; then
    continue
  fi
  echo "Running command: $CMD"
  eval "$CMD" < /dev/null
  COUNT=$((COUNT + 1))
done < "$COMMANDS_FILE"

if [ "$COUNT" -eq 0 ]; then
  echo "No commands found in $COMMANDS_FILE"
  exit 1
fi

echo "Ran $COUNT commands from $COMMANDS_FILE"
