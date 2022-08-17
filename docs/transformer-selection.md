## Transformer selection strategy

The TransformRegistry uses t-config to choose which Transformer will be
used. A transformer definition contains a supported list of source and
target Media Types. This is used for the most basic selection. It is further
refined by checking that the definition also supports transform options (the
parameters) that have been supplied in a transform request.

~~~
Transformer 1 defines options: Op1, Op2
Transformer 2 defines options: Op1, Op2, Op3, Op4

Transform request provides values for options: Op2, Op3
~~~
If we assume both transformers support the required source and target Media
Types, Transformer 2 will be selected because it knows about all the supplied
options. The definition may also specify that some options are required or
grouped. If any members of an optional group are supplied, all required
members of that group become required.

The configuration may impose a source file size limit resulting in the
selection of a different transformer. Size limits are normally added to avoid
the transforms consuming too many resources.

The configuration may also specify a priority which will be used in
Transformer selection if there are a number of possible transformers. The
highest priority is the one with the lowest number.