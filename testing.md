# Kinds of tests

## Unit
Specify behaviour of individual, small units, covering detailed edge cases. 

Mocking only own classes/interfaces.

Executed locally and in public CI environment.


## Integration
Test integration of own code with third party components where the latter do not get mocked.

In case of Google Drive client, integration tests configure it to talk to real Google Drive API using test account.

Mocking it was considered but rejected as too time consuming for not enough benefit.

Executed only locally to avoid sharing API credentials with public CI environment, risking their public disclosure.
Application is expected to remain small enough for local runs to never be too time consuming even when executed against
the real API.

This will prevent testing some of the use cases, especially Drive exception propagation, but, in context of this
application it's deemed acceptable. In the future it may be satisfied by using, say, proxy feature of
[WireMock](wiremock.org). For now, it will be mitigated by keeping the client wrapper light and simple, thus reducing
potential for bugs; more complex logic, e.g. exponential back-off on 'request rate exceeded error' will be delegated
to purely custom class extensively covered by unit tests.     


## End to end
Test integration of all layers, with only one/few key test cases covered.

Executed against real API for reasons the same as described for [Integration](#Integration) tests.
 


