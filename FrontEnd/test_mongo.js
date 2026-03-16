db = connect("mongodb://localhost:27017/satellite_platform");
printjson(db.users.find().toArray());
printjson(db.authorities.find().toArray());
