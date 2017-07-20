const path = require('path');
const express = require('express');
var exec = require('child_process').exec;
var nodeSsdp = require('node-ssdp');

var ip = require('ip').address();
const ssdpServerPort = 1900;
const restServerPort = 33333;
const descriptionFilePath = '/static/desc.xml';
const udn = 'uuid:362d9414-31a0-48b6-b684-2b4bdd8391d0';
const usn = 'urn:schemas-upnp-org:service:raspberry-pi:1';



/* Set up SSDP Server */

var SsdpServer = nodeSsdp.Server
  , ssdpServer = new SsdpServer({
    location: 'http://' + ip + ':' + restServerPort + descriptionFilePath,
    sourcePort: ssdpServerPort,
    udn: udn
  })
;

ssdpServer.addUSN(usn);

ssdpServer.on('advertise-alive', function (headers) {
  //todo: implement a device cache
});

ssdpServer.on('advertise-bye', function (headers) {
  //todo: implement a device cache
});



/* Set up REST server */

const restServer = express();

// Serve the description file for SSDP
restServer.use('/static', express.static(path.join(__dirname, 'public')));

restServer.get('/health', function (req, res) {
  console.log('health!');
  res.send('OK');
});

restServer.get('/speak', function (req, res) {
  console.log('speak!');
  var msg = req.query.msg;
  exec('espeak "' + msg + '"', function () {});
  res.send('Hello World!');
});

restServer.get('/playSound', function (req, res) {
  console.log('playSound');
  var track = req.query.track;

  switch (track) {
    case 'SingleChime':
      exec('aplay singleChime.wav', function () {});
      break;
    case 'DoubleChime':
      exec('aplay doubleChime.wav', function () {});
      break;
    case 'Doorbell':
      exec('aplay doorbell.wav', function () {});
      break;
    case 'Alarm':
      exec('aplay alarm.wav', function () {});
      break;
  }
  res.send('Hello World!');
});



/* Start the servers */

ssdpServer.start();
console.log('SSDP server listening on port ' + ssdpServerPort);

restServer.listen(restServerPort, function () {
  console.log('REST server listening on port ' + restServerPort);
});

process.on('exit', function(){
  ssdpServer.stop()
});
