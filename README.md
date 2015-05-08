# Auto-WEKA
Auto-WEKA is a tool for automating the selection of methods and hyperparameters of WEKA.

## Description
Many different machine learning algorithms exist that can easily be used off the shelf, many of these methods are implemented in the open source WEKA package. However, each of these algorithms have their own hyperparameters that can drastically change their performance, and there are a staggeringly large number of possible alternatives overall. Auto-WEKA considers the problem of simultaneously selecting a learning algorithm and setting its hyperparameters, going beyond previous methods that address these issues in isolation. Auto-WEKA does this using a fully automated approach, leveraging recent innovations in Bayesian optimization. Our hope is that Auto-WEKA will help non-expert users to more effectively identify machine learning algorithms and hyperparameter settings appropriate to their applications, and hence to achieve improved performance.

Project website: http://www.cs.ubc.ca/labs/beta/Projects/autoweka/

## People
 * Chris Thornton, M.Sc. Student (UBC)
 * Frank Hutter, Assistant Professor (Freiburg University)
 * Holger Hoos, Professor (UBC)
 * Kevin Leyton-Brown, Associate Professor (UBC)

## Papers
 * Chris Thornton, Frank Hutter, Holger Hoos, and Kevin Leyton-Brown. Auto-WEKA: Combined Selection and Hyperparameter Optimization of Classifiaction Algorithms. In Proc. of KDD 2013, 2013. [The main Auto-WEKA Paper]
 
## About this repository
I needed to extend Auto-WEKA for supporting Weka Filters, so I use this repository to share the changes.
 * Manuel Martín Salvador, PhD Student (Bournemouth University)

## License
GNU General Public License v3 (see [LICENSE](https://github.com/DraXus/autoweka/blob/master/LICENSE))
