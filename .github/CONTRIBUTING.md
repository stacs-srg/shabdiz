
# Guide to making a contribution

Every contribution starts from an [issue](https://github.com/stacs-srg/shabdiz/issues). Pick or define the issue you would like to resolve, and assign it to yourself. Once the issue is in place and assigned to you the code contribution can begin. 

The [flow](https://guides.github.com/introduction/flow/) of contribution is:

1. [Clone](https://help.github.com/articles/cloning-a-repository/) the repository on your local machine.
1. Create a branch named after the issue you are working to resolve. The branch must be prefixed with `issue-` followed by the ID of the issue. For example, the code contribution for an issue with the ID of `1` must be contained in a branch, named `issue-1`.
1. Work away on your branch, make all the changes you see fit. It is good practice to divide your work into commits of decent size with a meaningful message explaining what the changes represent. Don't worry if you have made a one character change commit with the message `Typo, Doh...`. You can [rebase](https://help.github.com/articles/about-git-rebase/) commits and tidy up the commit messages before requesting a PR review. 

1. Create a [Pull Request](https://help.github.com/articles/creating-a-pull-request/) based on `master` with your branch as the head. It is good practice to open the PR early, having defined a task list of things to be done in the PR description. This would allow others to follow the changes and make comments as you work to resolve the issue.

1. Once the work is done make sure:
    - the code is accompanied by sufficient tests.
    - the PR build passes. 
    - commit messages are coherent and meaningful. A good PR tells the story of its intent and how it came to be through its commit messages.
    - the PR description matches the [PR template](PULL_REQUEST_TEMPLATE.md).
    - the PR title matches the issue title it resolves.
    
    You can then request your PR to be reviewed. All PRs are required to be reviewed prior to merge.

## Ethos

- Leave code cleaner than you found it.
- Comments are failures to express what you mean in the form of code.
- "Premature optimisation is the root of all evil." -- [Donald Knuth](https://en.wikipedia.org/wiki/Donald_Knuth)
- "Perfection is achieved, not when there is nothing more to add, but when there is nothing left to take away." -- [Antoine de Saint-Exupery](https://en.wikipedia.org/wiki/Antoine_de_Saint-Exupéry)
- “There are two ways of constructing a software design: One way is to make it so simple that there are obviously no deficiencies, and the other way is to make it so complicated that there are no obvious deficiencies. The first method is far more difficult.” -- [Tony Hoare](https://en.wikipedia.org/wiki/Tony_Hoare)



Welcome.
