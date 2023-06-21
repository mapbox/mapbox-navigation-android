const package = require('./package.json');
const process = require('process');
const express = require('express');
const morgan = require('morgan');
const fetch = require('node-fetch');
const { URL } =  require('url');

const PORT = process.env.PORT || 3000;
const userAgent = `${package.name}/v${package.version} Node/${process.version}`;

const app = express();

app.use(morgan('dev'));

function rng(n) {
    return Math.floor(Math.random() * n) + 1;
}

const cache = new Map();

app.get('/directions/*', async (req, res) => {
    const url = new URL(`https://api.mapbox.com${req.url}`);

    const mapboxResponse = await fetch(url, { headers: {
        'User-Agent': userAgent + ' ' + req.headers['user-agent'],
    }});

    const mapboxJson = await mapboxResponse.json();

    cache.set(mapboxJson.uuid, mapboxJson.waypoints);

    const modifiedJson = {
        "mapbox": mapboxJson,
        "other": {
            "deliveries": rng(10),
        }
    };

    res.json(modifiedJson);
});

app.get('/directions-refresh/*', async (req, res) => {
    const url = new URL(`https://api.mapbox.com${req.url}`);

    const mapboxResponse = await fetch(url, { headers: {
        'User-Agent': userAgent + ' ' + req.headers['user-agent'],
    }});

    const uuid = req.url.split('/')[5];
    const waypoints = cache.get(uuid);
    
    const mapboxJson = await mapboxResponse.json();
    const modifiedJson = {
        "mapbox": mapboxJson,
        "other": {
            "wait_times": waypoints.map(() => rng(60 * 60 * 10)),
        }
    };

    res.json(modifiedJson);
});

app.listen(PORT, () => {
    console.log(`${package.name} listening on port ${PORT}...`);
});
