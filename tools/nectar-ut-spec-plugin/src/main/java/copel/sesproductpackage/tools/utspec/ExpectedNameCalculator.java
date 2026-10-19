package copel.sesproductpackage.tools.utspec;

import com.github.javaparser.ast.body.BodyDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.type.ArrayType;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.PrimitiveType;
import com.github.javaparser.ast.type.Type;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 型宣言から、テストの第1階層に書いてよい名前（対象メソッド・コンストラクタの名前）を求める。
 *
 * <p>名前の付け方は次のとおり。
 *
 * <ul>
 *   <li>オーバーロードされていないメソッドは、メソッド名をそのまま使う。
 *   <li>オーバーロードされているメソッドは「メソッド名_引数型」とする。引数が無い場合は、メソッド名だけにする。
 *   <li>コンストラクタは「コンストラクタ」。複数ある場合は「コンストラクタ_引数型」とする。 宣言が無い場合は、既定のコンストラクタとして「コンストラクタ」を使う。
 *   <li>引数型は、単純名を使い、配列は「Array」、可変長は「Varargs」を付ける（例: String[] は StringArray）。
 * </ul>
 *
 * @author Copel Co., Ltd.
 */
public class ExpectedNameCalculator {

  /** コンストラクタの名前。 */
  public static final String CONSTRUCTOR_NAME = "コンストラクタ";

  /**
   * 型宣言が持つメソッドとコンストラクタから、第1階層に書いてよい名前の一覧を求める。
   *
   * <p>private のメンバーも含める。テストは外側のクラスの公開された入口から網羅するが、 第1階層の名前は宣言の存在だけで判定するため、非公開の名前も対象に入れる。
   *
   * @param type 型宣言（クラス・インターフェース・enum・record）
   * @return 期待する名前の一覧
   */
  public List<String> calculate(TypeDeclaration<?> type) {
    Map<String, List<MethodDeclaration>> methodsByName = new LinkedHashMap<>();
    List<ConstructorDeclaration> constructors = new ArrayList<>();
    for (BodyDeclaration<?> member : type.getMembers()) {
      if (member.isMethodDeclaration()) {
        MethodDeclaration method = member.asMethodDeclaration();
        methodsByName
            .computeIfAbsent(method.getNameAsString(), key -> new ArrayList<>())
            .add(method);
      } else if (member.isConstructorDeclaration()) {
        constructors.add(member.asConstructorDeclaration());
      }
    }

    List<String> names = new ArrayList<>();
    for (Map.Entry<String, List<MethodDeclaration>> entry : methodsByName.entrySet()) {
      for (MethodDeclaration method : entry.getValue()) {
        boolean overloaded = entry.getValue().size() > 1;
        names.add(
            overloaded ? overloadName(entry.getKey(), method.getParameters()) : entry.getKey());
      }
    }
    if (constructors.isEmpty()) {
      names.add(CONSTRUCTOR_NAME);
    }
    for (ConstructorDeclaration constructor : constructors) {
      boolean overloaded = constructors.size() > 1;
      names.add(
          overloaded
              ? overloadName(CONSTRUCTOR_NAME, constructor.getParameters())
              : CONSTRUCTOR_NAME);
    }
    if (type.isRecordDeclaration()) {
      for (Parameter component : type.asRecordDeclaration().getParameters()) {
        String accessor = component.getNameAsString();
        if (!names.contains(accessor)) {
          names.add(accessor);
        }
      }
    }
    return names;
  }

  /**
   * オーバーロードされたメソッドやコンストラクタの名前を、引数型付きで求める。
   *
   * @param baseName 基本の名前（メソッド名またはコンストラクタ名）
   * @param parameters 引数の一覧
   * @return 引数が無ければ基本の名前、あれば「基本の名前_引数型1_引数型2」
   */
  public String overloadName(String baseName, List<Parameter> parameters) {
    if (parameters.isEmpty()) {
      return baseName;
    }
    String tokens = parameters.stream().map(this::parameterToken).collect(Collectors.joining("_"));
    return baseName + "_" + tokens;
  }

  /**
   * 引数1つを表す名前の断片を返す。可変長の引数には「Varargs」を付ける。
   *
   * @param parameter 引数
   * @return 引数の型の名前の断片
   */
  public String parameterToken(Parameter parameter) {
    String token = typeToken(parameter.getType());
    return parameter.isVarArgs() ? token + "Varargs" : token;
  }

  /**
   * 型の名前の断片を返す。ジェネリクスの型引数は含めず、単純名だけを使う。配列は次元ごとに「Array」を付ける。
   *
   * <p>メソッドの引数の型は、型引数の宣言（TypeParameter）ではなく参照型として解析されるため、参照型だけを扱う。
   *
   * @param type 型
   * @return 型の名前の断片
   */
  public String typeToken(Type type) {
    if (type.isArrayType()) {
      ArrayType arrayType = type.asArrayType();
      return typeToken(arrayType.getComponentType()) + "Array";
    }
    if (type.isPrimitiveType()) {
      PrimitiveType primitive = type.asPrimitiveType();
      return primitive.asString();
    }
    ClassOrInterfaceType reference = type.asClassOrInterfaceType();
    return reference.getNameAsString();
  }
}
