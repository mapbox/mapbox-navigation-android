import os
import sys

parts = sys.argv[1]

instrumentation_tests_path = 'instrumentation-tests/src/androidTest/java/com/mapbox/navigation/instrumentation_tests'


def file_has_tests(path, file):
	file_content = open(f'{path}/{file}').read()
	return '@Test' in file_content


def get_file_package(path):
	inner_path_part = path.replace(instrumentation_tests_path, '').replace('/', '.')
	return f'com.mapbox.navigation.instrumentation_tests{inner_path_part}'


test_matrix = {}
for root, dirs, files in os.walk(f'{instrumentation_tests_path}'):
	for file in files:
		if file_has_tests(root, file):
			test_matrix[f'{file}'] = get_file_package(root)

suite_size = len(test_matrix.keys()) // int(parts) + 1
suites = {}
for index, test in enumerate(test_matrix.keys()):
	part_number = int(index / suite_size) + 1
	if part_number in suites:
		suites[part_number].append(test)
	else:
		suites[part_number] = [test]

for part_number, tests in suites.items():
	tests_content = ''
	imports_content = ''
	for test in tests:
		tests_content += test.replace('.kt', '::class,\n')
		imports_content += f'import {test_matrix[test]}.{test.replace(".kt", "")}\n'
	suit_content = open('scripts/TestSuiteTemplate.kt').read()
	suit_content = suit_content.replace('{{PASTE TEST CLASSES HERE}}', tests_content)
	suit_content = suit_content.replace('{{PASTE IMPORTS HERE}}', imports_content)
	suit_content = suit_content.replace('{{PASTE PART NUMBER HERE}}', str(part_number))
	open(f'{instrumentation_tests_path}/TestSuitePart{part_number}.kt', 'w').write(suit_content)
