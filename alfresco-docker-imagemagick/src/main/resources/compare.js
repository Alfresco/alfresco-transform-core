#!/usr/bin/node
const compareImages = require('resemblejs/compareImages');
const fs = require("mz/fs");
 

// npm i mz
// npm i resemblejs

async function getDiff(img1Path, img2Path){
    const options = {
        output: {
            errorColor: {
                red: 255,
                green: 0,
                blue: 255
            },
            errorType: 'movement',
            transparency: 0.3,
            largeImageThreshold: 1200,
            useCrossOrigin: false,
            outputDiff: true
        },
        scaleToSameSize: true,
        ignore: ['nothing', 'less', 'antialiasing', 'colors', 'alpha'],
    };
 
    // The parameters can be Node Buffers
    // data is the same as usual with an additional getBuffer() function
    const data = await compareImages(
        await fs.readFile(img1Path),
        await fs.readFile(img2Path),
        options
    );

    console.log(data);
 
    await fs.writeFile('./output.png', data.getBuffer());
    await fs.writeFile('./output.json', JSON.stringify(data));
}
 
getDiff(process.argv[2], process.argv[3]);
//getDiff('/Users/p3700621/Downloads/44174363_1996471110414725_5527179798176071680_o.jpg', '/Users/p3700621/Downloads/44300765_1996444827084020_2153540260556963840_o.jpg');