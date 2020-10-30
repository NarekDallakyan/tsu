package social.tsu.android.validation

interface ValidatedField<VC,EC,V>{
    fun validateField(
        valueComponent: VC,
        errorComponent: EC,
        predicate:(value: V, message: String?) -> ValidationResult
    ): Boolean

    fun handleResult(result: ValidationResult, valueComponent: VC, errorComponent: EC)
}

