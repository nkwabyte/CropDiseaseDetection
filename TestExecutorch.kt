import org.pytorch.executorch.Module
import org.pytorch.executorch.Tensor
import org.pytorch.executorch.EValue

fun main() {
    println(Module::class.java.methods.joinToString("\n") { it.name })
}
