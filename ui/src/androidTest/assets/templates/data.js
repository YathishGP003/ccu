var dataG3 = [{ "x": 2, "y": 0},
{ "x": 3, "y": 1},
{ "x": 4, "y": 0}]

var dataG1 = [{
                 type: "stepLine",
                 color: "red",
                     dataPoints :[{ x: 1, y: 0, indexLabel:"relay1",markerColor: "red" ,color:"red" }, //dataPoint
                         { x: 2, y: 0},
                         { x: 3, y: 1},
                         { x: 4, y: 0}]
                 },
                 {
                 type: "stepLine",
                     dataPoints :[{ x: 1, y: 0, indexLabel:"room-temp",markerColor: "green"  }, //dataPoint
                         { x: 2, y: 7.3,indexLabel:"",markerColor: "green" },
                         { x: 3, y: 1, indexLabel:"",markerColor: "green" },
                         { x: 4, y: 0,indexLabel:"",markerColor: "green" }]
             }]