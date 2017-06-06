'''
Utility to build and generate a new branch in android-docs containing the javadoc for this project.
'''

import click
import json
import os
import requests
import subprocess
import sys

# We get the default version from here
NAVIGATION_ROOT_PATH = '../navigation'
ANDROID_DOCS_ROOT_PATH = '../../android-docs'
GRADLE_PROPERTIES_PATH = '%s/gradle.properties' % NAVIGATION_ROOT_PATH
GRADLE_TOKEN = 'VERSION_NAME='

def update_current_version(file_path, file_var, version):
	dirty = False
	click.echo('Updating file to version %s: %s.' % (version, file_path))
	with open(file_path, 'r') as f:
		file_lines = f.readlines()
	for line_number in range(len(file_lines)):
		if file_lines[line_number].startswith(file_var):
			content_old = file_lines[line_number]
			content_new = '%s%s\n' % (file_var, version)
			if (content_old != content_new):
				click.echo('%s -> %s' % (content_old.strip(), content_new.strip()))
				file_lines[line_number] = content_new
				dirty = True
	if dirty:
		with open(file_path, 'w') as f:
			f.writelines(file_lines)
	else:
		click.echo('File already has the right version.')
	return dirty


version = raw_input("Enter version number: ")
print "Version being used: ", version
dirty_gradle = update_current_version(file_path=GRADLE_PROPERTIES_PATH, file_var=GRADLE_TOKEN, version=version)
subprocess.Popen(['./gradlew', 'javadocrelease'], cwd=NAVIGATION_ROOT_PATH).wait()
subprocess.Popen(['mv', 'release', version], cwd='../navigation/libandroid-navigation/build/docs/javadoc/').wait()
subprocess.Popen(['git', 'checkout', 'mb-pages'], cwd=ANDROID_DOCS_ROOT_PATH).wait()
BRANCH_NAME = version + '-javadoc'
print "Creating android-docs branch: ", BRANCH_NAME
subprocess.Popen(['git', 'checkout', '-b', BRANCH_NAME], cwd='../../android-docs').wait()
subprocess.Popen(['mv', version, '../../../../../../android-docs/api/navigation-sdk'], cwd='../navigation/libandroid-navigation/build/docs/javadoc/').wait()
subprocess.Popen(['git', 'add', 'api/navigation-sdk'], cwd=ANDROID_DOCS_ROOT_PATH).wait()
COMMIT_MESSAGE = version + "-javadoc-added"
print "Committing with message: ", COMMIT_MESSAGE
subprocess.Popen(['git', 'commit', '-m', COMMIT_MESSAGE], cwd=ANDROID_DOCS_ROOT_PATH).wait()
subprocess.Popen(['git', 'push', '-u', 'origin', BRANCH_NAME], cwd=ANDROID_DOCS_ROOT_PATH).wait()
print "Commit pushed, open a PR now"
