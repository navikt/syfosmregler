package no.nav.syfo.rules.tilbakedatering

import no.nav.syfo.model.Status

class RuleTree(

) {
    private fun ok(): RuleNode = StatusNode(Status.OK)
    private fun manual() = StatusNode(Status.MANUAL_PROCESSING)
    private fun invalid() = StatusNode(Status.INVALID)

    fun tilbakedatering(): RuleNode = Tilbakedatering(
        yes = Ettersending(
            yes = ok(),
            no = TilbakedateringIntill8Dager(
                yes = tilbakedateringIntill8Dager(), no = TilbakedateringIntill30Dager(
                    yes = tilbakedateringIntill30Dager(), no = tilbakedateringOver30Dager()
                )
            ),
        ), no = ok()
    )

    private fun tilbakedateringIntill30Dager(): RuleNode = Begrunnelse(
        yes = Forlengelse(
            yes = ok(), no = ArbeigsgiverPeriode(
                yes = ok(), no = SpesialistHelsetjenesten(
                    yes = ok(), no = manual()
                )
            )
        ), no = SpesialistHelsetjenesten(
            yes = manual(), no = invalid()
        )
    )

    private fun tilbakedateringIntill8Dager(): RuleNode = BegrunnelseKontaktDato(
        yes = ok(), no = Forlengelse(
            yes = ok(), no = SpesialistHelsetjenesten(
                yes = ok(), no = invalid()
            )
        )
    )

    private fun tilbakedateringOver30Dager() = Begrunnelse(
        yes = manual(), no = SpesialistHelsetjenesten(
            yes = manual(), no = invalid()
        )
    )
}