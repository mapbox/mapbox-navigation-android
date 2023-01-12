import argparse
import os

parser = argparse.ArgumentParser(description='Add a new changelog file')

parser.add_argument('-f', '--feature', nargs='+', help='Features')
parser.add_argument('-b', '--bugfix', nargs='+', help='Bug fixes and improvements')
parser.add_argument('-i', '--issue', nargs='+', help='Known issues :warning:')
parser.add_argument('-o', '--other', nargs='+', help='Other changes')

args = parser.parse_args()


def write_file(changes, dir):
    filename = 'changelog/unreleased/' + dir + '/changes.md'
    os.makedirs(os.path.dirname(filename), exist_ok=True)
    prepared_changes = ''
    for change in changes:
        prepared_changes += '- ' + change + '\n'
    open(filename, 'w').write(prepared_changes)


if args.feature:
    write_file(args.feature, 'features')

if args.bugfix:
    write_file(args.bugfix, 'bugfixes')

if args.issue:
    write_file(args.issue, 'issues')

if args.other:
    write_file(args.other, 'other')
