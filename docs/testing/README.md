[index](../../)

# Testing

## Requirements

assuming you have a `.lock.json` file ready to use from
[Build](../building)

## MultiMC

Disclaimer: this functionality is not supported by the MultiMC devs, so do not
annoy them with bugreports about voodoo.. come to me (NikkyAI) instead

on windows it is required that multimc is available on the path
and multimc is closed prior to running the command

those restrictions do not apply on linux (and i have no mac to test)

now with all that out of the way.. lets run the pack locally


```bash
vodoo test mmc awesomepack.lock.json
```

to force a clean install of the pack you can use

```bash
vodoo test mmc awesomepack.lock.json --clean
```

## SK Creator Tools

this assumes prior knowledge of skcraft creator tools

you can package the modpack for skcraft

````bash
voodoo pack sk awesomepack.lock.json
````

this will create a `workspace` folder
you can now open the workspace with the skcraft creator tools as usual


this is all fow now

TODO: add package.md and deploy.md