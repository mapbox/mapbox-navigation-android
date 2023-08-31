import os
import sys

parts = sys.argv[1]

instrumentation_tests_path = 'instrumentation-tests/src/androidTest/java/com/mapbox/navigation/instrumentation_tests'
core_tests = os.listdir(f'{instrumentation_tests_path}/core')
ui_tests = os.listdir(f'{instrumentation_tests_path}/ui')

core_part_size = int(len(core_tests) / int(parts)) + 1

suites = {}
for index, test in enumerate(core_tests):
	part_number = int(index / core_part_size) + 1
	if part_number in suites:
		suites[part_number].append(test)
	else:
		suites[part_number] = [test]

for key, value in suites.items():
	tests_content = ''
	for test in value:
		tests_content += test.replace('.kt', '::class,\n')
	suit_content = open('scripts/CoreTestSuiteTemplate.kt').read()
	suit_content = suit_content.replace('{{PASTE TEST CLASSES HERE}}', tests_content)
	suit_content = suit_content.replace('{{PASTE PART NUMBER HERE}}', str(key))
	open(f'{instrumentation_tests_path}/Core{key}PartTestSuite.kt', 'w').write(suit_content)

imports = ''
ui_tests = ''
for root, dirs, files in os.walk(f'{instrumentation_tests_path}/ui'):
	imports += f'import {root.replace("instrumentation-tests/src/androidTest/java/", "").replace("/", ".")}.*\n'
	for file in files:
		ui_tests += file.replace('.kt', '::class,\n')

	suit_content = open('scripts/UiTestSuiteTemplate.kt').read()
	suit_content = suit_content.replace('{{PASTE TEST CLASSES HERE}}', ui_tests)
	suit_content = suit_content.replace('{{PASTE IMPORTS HERE}}', imports)
	open(f'{instrumentation_tests_path}/UiTestSuite.kt', 'w').write(suit_content)
