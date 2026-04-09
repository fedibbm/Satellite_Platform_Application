(async () => { console.log(JSON.stringify(await (await fetch('http://localhost:8080/api/geospatial/images/by-project/69b78c4a84723a565a35db2f?page=0&size=10')).json(), null, 2)) })()
