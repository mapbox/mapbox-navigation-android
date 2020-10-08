#!/usr/bin/env node
/* eslint-disable no-console */
const fs = require('fs');
const path = require('path');
const globby = require('globby');
const { basename } = require('path');

/**
 * Pass the version number (X.X.X)
 * */
let VERSION = process.argv[2];
let FLAVOR = process.argv[3];
if (!VERSION || !/^(\d|\.)+(-(alpha|beta|rc|pre)\.\d)?/.test(VERSION)) {
  throw new Error(`❌ You must include a valid version number.`);
}

if (!FLAVOR || !/^(core|ui)/.test(FLAVOR)) {
    throw new Error(`❌ You must specify "core" or "ui".`);
}

/**
 * Prepare paths.
 * */
const basePath = path.resolve(
  __dirname,
  `../${FLAVOR}/${VERSION}`
);
const glob = `${basePath}/**/*.html`;

/**
 * Prepare custom HTML content to be added.
 * */
const topBarContent = (depth) => {
  return `<body>\n<div class="top-bar">\n<div class="limiter">\n<span class="shell-mb-logo"></span>\n<span class="product-name">Navigation SDK for Android Reference (${VERSION})</span>\n</div>\n</div>\n<div class="limiter">\n<a href="${
    depth === 1 ? './' : '../'
  }${depth > 2 ? '../' : ''}${depth > 3 ? '../' : ''}${
    depth > 4 ? '../' : ''
  }index.html">All modules</a>&nbsp;/&nbsp;`;
};
const allModulesPageContent = (folders) => {
  return `<html>\n<head>\n<meta charset="UTF-8">\n<title>API reference | Mapbox Navigation SDK for Android</title>\n<link rel="stylesheet" href="style.css">\n</head>\n${topBarContent(
    6
  )}\n<h1>Mapbox Navigation SDK for Android</h1>\n<h2>Modules</h2>\n<table>\n<tbody>\n${folders.join(
    '\n'
  )} \n</tbody>\n</table>\n</div>\n</body>\n</html>`;
};

/**
 * Add header and bread crumbs to every generated HTML page.
 * */
globby(glob).then((htmlFiles) => {
  htmlFiles.forEach((filePath) => {
    const relativeFilePath = filePath.split(`${VERSION}/`)[1];
    const depth = relativeFilePath.split('/').length;
    fs.readFile(filePath, 'utf8', (err, data) => {
      if (err) throw err;
      let result = data.replace(/<BODY>/, topBarContent(depth));
      result = result.replace(/<\/BODY>/, `</div>\n</body>`);
      if (result) {
        fs.writeFile(filePath, result, 'utf8', function (err) {
          if (err) return console.log(err);
          console.log(`Added top-bar to ${filePath}`);
        });
      }
    });
  });
});

/**
 * Create a new HTML page that lists/links to all modules.
 * */
fs.readdir(basePath, (err, subFolders) => {
  const folders = subFolders
    .filter((folder) => {
      return folder.indexOf('.') < 0;
    })
    .map((module) => {
      return `<tr><td><h3><a href="${module}/index.html">${module}</a></h3></td></tr>`;
    });
  fs.writeFile(
    `${basePath}/index.html`,
    allModulesPageContent(folders),
    (err) => {
      if (err) throw err;
      console.log(`Created index.html for ${VERSION}`);
    }
  );
  fixPackageList();
});

function fixPackageList() {
  const topLevelIndexPath = `${basePath}/index.html`;
  fs.readFile(topLevelIndexPath, 'utf8', (err, data) => {
    if (err) throw err;
    const result = data.replace(/package-list\/index\.html/, 'package-list');
    if (result) {
      fs.writeFile(topLevelIndexPath, result, 'utf8', function(err) {
        if (err) throw err;
        console.log(`Updated link to package list.`);
      });
    }
  });
}
