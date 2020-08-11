# aws-transcribe-clj

Sample web application converting speech to text using AWS Transcribe.

## Installation

1. Edit shadow-cljs.edn file by replacing values corresponding to AWS and Facebook settings. 

2. Download AWS SDK [here](https://sdk.amazonaws.com/builder/js/), and store it store locally in resources/public/js/aws/sdk-\<version>.js
    * NOTE: Include only S3 and TranscribeService into the build.
3. Replace the version of AWS SDK in index.html

4. Install npm dependecies by running `npm install`.

## Usage

Run development environment:

    $ clj -A:dev

Release the app:

    $ clj -A:prod

Serve release build locally:
    
    $ ./scripts/serve-release.sh
