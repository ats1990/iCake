package com.example.icake.activity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.icake.R;
import com.example.icake.helper.ConfiguracaoFirebase;
import com.example.icake.helper.UsuarioFirebase;
import com.example.icake.model.Empresa;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Picasso;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

public class ConfiguracoesEmpresaActivity extends AppCompatActivity {

    private EditText editEmpresaNome, editEmpresaCategoria,
            editEmpresaTempo, editEmpresaTaxa;
    private ImageView imagePerfilEmpresa;

    private static final int SELECAO_GALERIA = 200;
    private StorageReference storageReference;
    private DatabaseReference firebaseRef;
    private String idUsuarioLogado;
    private String urlImagemSelecionada = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_configuracoes_empresa);

        //Configurações iniciais
        inicializarComponentes();
        storageReference = ConfiguracaoFirebase.getFirebaseStorage();
        firebaseRef = ConfiguracaoFirebase.getFirebase();
        idUsuarioLogado = UsuarioFirebase.getIdUsuario();

        //Configurações Toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle("Configurações");
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        imagePerfilEmpresa.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(
                        Intent.ACTION_PICK,
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                );
                if( i.resolveActivity(getPackageManager()) != null ){
                    startActivityForResult(i, SELECAO_GALERIA);
                }
            }
        });

        /*Recuperar dados da empresa*/
        recuperarDadosEmpresa();


    }

    private void inicializarComponentes(){
        editEmpresaNome = findViewById(R.id.editEmpresaNome);
        editEmpresaCategoria = findViewById(R.id.editEmpresaCategoria);
        editEmpresaTaxa = findViewById(R.id.editEmpresaTaxa);
        editEmpresaTempo = findViewById(R.id.editEmpresaTempo);
        imagePerfilEmpresa = findViewById(R.id.imagePerfilEmpresa);
    }

    private void recuperarDadosEmpresa(){

        DatabaseReference empresaRef = firebaseRef
                .child("empresas")
                .child( idUsuarioLogado );
        empresaRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {

                if( dataSnapshot.getValue() != null ){
                    Empresa empresa = dataSnapshot.getValue(Empresa.class);
                    editEmpresaNome.setText(empresa.getNome());
                    editEmpresaCategoria.setText(empresa.getCategoria());
                    editEmpresaTaxa.setText(empresa.getPrecoEntrega().toString());
                    editEmpresaTempo.setText(empresa.getTempo());

                    urlImagemSelecionada = empresa.getUrlImagem();

                    if( urlImagemSelecionada != "" ){
                        Picasso.get()
                                .load(urlImagemSelecionada)
                                .into(imagePerfilEmpresa);
                    }

                }

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

    }

    public void validarDadosEmpresa(View view){

        //Valida se os campos foram preenchidos
        String nome = editEmpresaNome.getText().toString();
        String taxa = editEmpresaTaxa.getText().toString();
        String categoria = editEmpresaCategoria.getText().toString();
        String tempo = editEmpresaTempo.getText().toString();

        if( !nome.isEmpty()){
            if( !taxa.isEmpty()){
                if( !categoria.isEmpty()){
                    if( !tempo.isEmpty()){

                        Empresa empresa = new Empresa();
                        empresa.setIdUsuario( idUsuarioLogado );
                        empresa.setNome( nome );
                        empresa.setPrecoEntrega( Double.parseDouble(taxa) );
                        empresa.setCategoria(categoria);
                        empresa.setTempo( tempo );
                        empresa.setUrlImagem( urlImagemSelecionada );
                        empresa.salvar();
                        finish();

                    }else{
                        exibirMensagem("Digite um tempo de entrega");
                    }
                }else{
                    exibirMensagem("Digite uma categoria");
                }
            }else{
                exibirMensagem("Digite uma taxa de entrega");
            }
        }else{
            exibirMensagem("Digite um nome para a empresa");
        }

    }

    private void exibirMensagem(String texto){
        Toast.makeText(this, texto, Toast.LENGTH_SHORT)
                .show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {
            Bitmap imagem = null;

            try {

                switch (requestCode) {
                    case SELECAO_GALERIA:
                        Uri localImagem = data.getData();
                        imagem = MediaStore.Images
                                .Media
                                .getBitmap(
                                        getContentResolver(),
                                        localImagem
                                );
                        break;
                }

                if (imagem != null) {

                    imagePerfilEmpresa.setImageBitmap(imagem);

                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    imagem.compress(Bitmap.CompressFormat.JPEG, 70, baos);
                    byte[] dadosImagem = baos.toByteArray();

                    StorageReference imagemRef = storageReference
                            .child("imagens")
                            .child("empresas")
                            .child(idUsuarioLogado + "jpeg");

                    UploadTask uploadTask = imagemRef.putBytes(dadosImagem);
                    uploadTask.continueWithTask(new Continuation<UploadTask.TaskSnapshot, Task<Uri>>() {
                        @Override
                        public Task<Uri> then(@NonNull Task<UploadTask.TaskSnapshot> task) throws Exception {

                            if (!task.isSuccessful()) {
                                throw task.getException();
                            }
                            return imagemRef.getDownloadUrl();
                        }
                    }).addOnCompleteListener(new OnCompleteListener<Uri>() {
                        @Override
                        public void onComplete(@NonNull Task<Uri> task) {
                            if (task.isSuccessful()) {
                                Uri downloadUrl = task.getResult();
                                urlImagemSelecionada = downloadUrl.toString();

                                Toast.makeText(ConfiguracoesEmpresaActivity.this, "Sucesso ao fazer upload da imagem", Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(ConfiguracoesEmpresaActivity.this, "Erro ao fazer upload da imagem", Toast.LENGTH_SHORT).show();
                            }
                        }
                    });

                }

            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }

        }


    }
}


