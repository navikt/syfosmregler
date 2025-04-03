package no.nav.syfo.rules.pdl.client.model

private val GET_PERSON_QUERY =
    """
    query(${"$"}ident: ID!){
      hentIdenter(ident: ${"$"}ident, historikk: false) {
        identer {
          ident,
          historisk,
          gruppe
        }
      }
      hentPerson(ident: ${"$"}ident) {
          foedselsdato{
              foedselsdato
          }
      }
    }
"""
        .trimIndent()
        .replace(Regex("[\n\t]"), "")

data class GetPersonRequest(
    val variables: GetPersonVeriables,
    val query: String = GET_PERSON_QUERY,
)
